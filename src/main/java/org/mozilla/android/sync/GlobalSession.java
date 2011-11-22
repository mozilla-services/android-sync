// vim: ts=2:sw=2:expandtab:
/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Android Sync Client.
 *
 * The Initial Developer of the Original Code is
 * the Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Richard Newman <rnewman@mozilla.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.android.sync;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.mozilla.android.sync.crypto.KeyBundle;
import org.mozilla.android.sync.net.SyncStorageRequest;
import org.mozilla.android.sync.net.SyncStorageRequestDelegate;
import org.mozilla.android.sync.net.SyncStorageResponse;
import org.mozilla.android.sync.stage.CheckPreconditionsStage;
import org.mozilla.android.sync.stage.CompletedStage;
import org.mozilla.android.sync.stage.EnsureClusterURLStage;
import org.mozilla.android.sync.stage.GlobalSyncStage;
import org.mozilla.android.sync.stage.GlobalSyncStage.Stage;
import org.mozilla.android.sync.stage.NoSuchStageException;


public class GlobalSession {
  public static final String API_VERSION = "1.1";
  public Stage currentState = Stage.idle;

  private URI clusterURL;
  private String username;
  private String password;
  private KeyBundle syncKeyBundle;
  private GlobalSessionCallback callback;

  public class JSONFetchDelegate implements SyncStorageRequestDelegate {
    private JSONObjectCallback callback;

    JSONFetchDelegate(JSONObjectCallback callback) {
      this.callback = callback;
    }

    public String credentials() {
      return username + ":" + password;
    }

    public String ifUnmodifiedSince() {
      return null;
    }

    public void handleSuccess(SyncStorageResponse res) {
      try {
        callback.handleSuccess(res.jsonObjectBody());
      } catch (Exception e) {
        callback.handleError(e);
      }
    }

    public void handleFailure(SyncStorageResponse response) {
      // TODO
    }

    public void handleError(IOException e) {
      // TODO
    }
  }

  public interface JSONObjectCallback {
    public void handleSuccess(ExtendedJSONObject result);
    public void handleFailure(Object reason);
    public void handleError(Exception e);
  }

  private boolean isInvalidString(String s) {
    return s == null ||
           s.trim().length() == 0;
  }

  private boolean anyInvalidStrings(String s, String...strings) {
    if (isInvalidString(s)) {
      return true;
    }
    for (String str : strings) {
      if (isInvalidString(str)) {
        return true;
      }
    }
    return false;
  }

  public GlobalSession(String clusterURL, String username, String password, KeyBundle syncKeyBundle, GlobalSessionCallback callback) throws SyncConfigurationException, IllegalArgumentException {
    if (callback == null) {
      throw new IllegalArgumentException("Must provide a callback to GlobalSession constructor.");
    }

    if (anyInvalidStrings(username, password)) {
      throw new SyncConfigurationException();
    }

    URI clusterURI;
    try {
      clusterURI = (clusterURL == null) ? null : new URI(clusterURL);
    } catch (URISyntaxException e) {
      throw new SyncConfigurationException();
    }

    if (syncKeyBundle == null ||
        syncKeyBundle.getEncryptionKey() == null ||
        syncKeyBundle.getHMACKey() == null) {
      throw new SyncConfigurationException();
    }

    this.setClusterURL(clusterURI);

    this.username      = username;
    this.password      = password;
    this.syncKeyBundle = syncKeyBundle;
    this.callback      = callback;
    prepareStages();
  }

  private Map<Stage, GlobalSyncStage> stages;
  private void prepareStages() {
    stages = new HashMap<Stage, GlobalSyncStage>();
    stages.put(Stage.checkPreconditions, new CheckPreconditionsStage());
    stages.put(Stage.ensureClusterURL,   new EnsureClusterURLStage());
    stages.put(Stage.completed,          new CompletedStage());
  }

  private GlobalSyncStage getStageByName(Stage next) throws NoSuchStageException {
    GlobalSyncStage stage = stages.get(next);
    if (stage == null) {
      throw new NoSuchStageException(next);
    }
    return stage;
  }

  protected void fetchJSON(String url, JSONObjectCallback callback) throws URISyntaxException {
    SyncStorageRequest r = new SyncStorageRequest(url);
    r.delegate = new JSONFetchDelegate(callback);
    r.get();
  }

  protected void fetchMetaGlobal(JSONObjectCallback callback) throws URISyntaxException {
    String metaURL = this.clusterURL + GlobalSession.API_VERSION + "/" + this.username + "/storage/meta/global";
    this.fetchJSON(metaURL, callback);
  }

  /**
   * Advance and loop around the stages of a sync.
   * @param current
   * @return
   */
  public static Stage nextStage(Stage current) {
    int index = current.ordinal() + 1;
    int max   = Stage.completed.ordinal() + 1;
    return Stage.values()[index % max];
  }

  /**
   * Move to the next stage in the syncing process.
   * @param next
   *        The next stage.
   * @throws NoSuchStageException if the stage does not exist.
   */
  public void advance() throws NoSuchStageException {
    this.callback.handleStageCompleted(this.currentState, this);
    Stage next = nextStage(this.currentState);
    GlobalSyncStage nextStage = this.getStageByName(next);
    this.currentState = next;
    nextStage.execute(this);
  }

  /**
   * Begin a sync.
   *
   * The caller is responsible for:
   *
   * * Verifying that any backoffs/minimum next sync are respected
   * * Ensuring that the device is online
   * * Ensuring that dependencies are ready
   *
   * @throws AlreadySyncingException
   *
   */
  public void start() throws AlreadySyncingException {
    if (this.currentState != GlobalSyncStage.Stage.idle) {
      throw new AlreadySyncingException(this.currentState);
    }
    try {
      this.advance();
    } catch (NoSuchStageException ex) {
      // This should not occur.
      // TODO: log.
      this.callback.handleError(this, ex);
    }
  }

  public void completeSync() {
    this.currentState = GlobalSyncStage.Stage.idle;
    this.callback.handleSuccess(this);
  }

  public URI getClusterURL() {
    return clusterURL;
  }
  public void setClusterURL(URI u) {
    this.clusterURL = u;
  }
  public void setClusterURL(String u) throws URISyntaxException {
    this.setClusterURL((u == null) ? null : new URI(u));
  }

}
