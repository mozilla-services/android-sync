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
import java.net.URISyntaxException;

import org.mozilla.android.sync.crypto.KeyBundle;
import org.mozilla.android.sync.net.SyncStorageRequest;
import org.mozilla.android.sync.net.SyncStorageRequestDelegate;
import org.mozilla.android.sync.net.SyncStorageResponse;

public class GlobalSession {
  private String clusterURL;
  private String username;
  private String password;
  private KeyBundle syncKeyBundle;

  public static final String API_VERSION = "1.1";

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

  public GlobalSession(String clusterURL, String username, String password, KeyBundle syncKeyBundle) {
    this.clusterURL    = clusterURL;
    this.username      = username;
    this.password      = password;
    this.syncKeyBundle = syncKeyBundle;
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
}
