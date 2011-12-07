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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.mozilla.android.sync.crypto.CryptoException;
import org.mozilla.android.sync.crypto.KeyBundle;
import org.mozilla.android.sync.net.InfoCollections;
import org.mozilla.android.sync.net.InfoCollectionsDelegate;
import org.mozilla.android.sync.net.MetaGlobal;
import org.mozilla.android.sync.net.MetaGlobalDelegate;
import org.mozilla.android.sync.net.SyncStorageRecordRequest;
import org.mozilla.android.sync.net.SyncStorageRequest;
import org.mozilla.android.sync.net.SyncStorageRequestDelegate;
import org.mozilla.android.sync.net.SyncStorageResponse;
import org.mozilla.android.sync.repositories.Utils;
import org.mozilla.android.sync.stage.AndroidBrowserBookmarksServerSyncStage;
import org.mozilla.android.sync.stage.CheckPreconditionsStage;
import org.mozilla.android.sync.stage.CompletedStage;
import org.mozilla.android.sync.stage.EnsureClusterURLStage;
import org.mozilla.android.sync.stage.EnsureKeysStage;
import org.mozilla.android.sync.stage.FetchInfoCollectionsStage;
import org.mozilla.android.sync.stage.FetchMetaGlobalStage;
import org.mozilla.android.sync.stage.GlobalSyncStage;
import org.mozilla.android.sync.stage.GlobalSyncStage.Stage;
import org.mozilla.android.sync.stage.NoSuchStageException;

import android.content.Context;
import android.util.Log;

public class GlobalSession implements CredentialsSource {
  public static final String API_VERSION   = "1.1";
  public static final long STORAGE_VERSION = 5;

  public Stage currentState = Stage.idle;

  private static final String LOG_TAG = "GlobalSession";

  // These public fields and methods are for the use of Stage handlers.
  public URI clusterURL;
  public String username;
  public KeyBundle syncKeyBundle;
  private CollectionKeys collectionKeys;

  public InfoCollections infoCollections;      // TODO: persist historical timestamps.
  public MetaGlobal metaGlobal;

  public void setCollectionKeys(CollectionKeys k) {
    collectionKeys = k;
  }
  @Override
  public CollectionKeys getCollectionKeys() {
    return collectionKeys;
  }
  @Override
  public KeyBundle keyForCollection(String collection) throws NoCollectionKeysSetException {
    return this.getCollectionKeys().keyBundleForCollection(collection);
  }

  @Override
  public String credentials() {
    return username + ":" + password;
  }

  public URI collectionURI(String collection, boolean full) throws URISyntaxException {
    // Do it this way to make it easier to add more params later.
    // It's pretty ugly, I'll grant.
    boolean anyParams = full;
    String  uriParams = "";
    if (anyParams) {
      StringBuilder params = new StringBuilder("?");
      if (full) {
        params.append("full=1");
      }
      uriParams = params.toString();
    }
    String uri = this.storageURL(true) + collection + uriParams;
    return new URI(uri);
  }

  public String storageURL(boolean trailingSlash) {
    return this.clusterURL + "1.1/" + this.username +
           (trailingSlash ? "/storage/" : "/storage");
  }

  public URI wboURI(String collection, String id) throws URISyntaxException {
    return new URI(this.storageURL(true) + collection + "/" + id);
  }

  protected URI getKeysURI() throws URISyntaxException {
    return wboURI("crypto", "keys");
  }

  private String password;
  private GlobalSessionCallback callback;

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

  private Context context;
  public Context getContext() {
    return this.context;
  }

  public GlobalSession(String clusterURL, String username, String password, KeyBundle syncKeyBundle, GlobalSessionCallback callback, Context context) throws SyncConfigurationException, IllegalArgumentException {
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
    this.context       = context;
    prepareStages();
  }

  protected Map<Stage, GlobalSyncStage> stages;
  private String syncID;
  protected void prepareStages() {
    stages = new HashMap<Stage, GlobalSyncStage>();
    stages.put(Stage.checkPreconditions,      new CheckPreconditionsStage());
    stages.put(Stage.ensureClusterURL,        new EnsureClusterURLStage());
    stages.put(Stage.fetchInfoCollections,    new FetchInfoCollectionsStage());
    stages.put(Stage.fetchMetaGlobal,         new FetchMetaGlobalStage());
    stages.put(Stage.ensureKeysStage,         new EnsureKeysStage());

    // Sync collections. Hard-code for now.
    stages.put(Stage.syncBookmarks,           new AndroidBrowserBookmarksServerSyncStage());
    stages.put(Stage.completed,               new CompletedStage());
  }

  protected GlobalSyncStage getStageByName(Stage next) throws NoSuchStageException {
    GlobalSyncStage stage = stages.get(next);
    if (stage == null) {
      throw new NoSuchStageException(next);
    }
    return stage;
  }

  private String getMetaURL() {
    return this.clusterURL + GlobalSession.API_VERSION + "/" + this.username + "/storage/meta/global";
  }

  public void fetchMetaGlobal(MetaGlobalDelegate callback) throws URISyntaxException {
    if (this.metaGlobal == null) {
      String metaURL = getMetaURL();
      this.metaGlobal = new MetaGlobal(metaURL, credentials());
    }
    this.metaGlobal.fetch(callback);
  }

  public void fetchInfoCollections(InfoCollectionsDelegate callback) throws URISyntaxException {
    if (this.infoCollections == null) {
      String infoURL = this.clusterURL + GlobalSession.API_VERSION + "/" + this.username + "/info/collections";
      this.infoCollections = new InfoCollections(infoURL, credentials());
    }
    this.infoCollections.fetch(callback);
  }


  protected void uploadKeys(CryptoRecord keysRecord,
                            final KeyUploadDelegate keyUploadDelegate) {
    SyncStorageRecordRequest request;
    final GlobalSession globalSession = this;
    try {
      request = new SyncStorageRecordRequest(this.getKeysURI());
    } catch (URISyntaxException e) {
      keyUploadDelegate.onKeyUploadFailed(e);
      return;
    }

    request.delegate = new SyncStorageRequestDelegate() {

      @Override
      public String ifUnmodifiedSince() {
        return null;
      }

      @Override
      public void handleRequestSuccess(SyncStorageResponse response) {
        keyUploadDelegate.onKeysUploaded();
      }

      @Override
      public void handleRequestFailure(SyncStorageResponse response) {
        keyUploadDelegate.onKeyUploadFailed(new HTTPFailureException(response));
      }

      @Override
      public void handleRequestError(Exception ex) {
        keyUploadDelegate.onKeyUploadFailed(ex);
      }

      @Override
      public String credentials() {
        return globalSession.credentials();
      }
    };

    keysRecord.setKeyBundle(syncKeyBundle);
    try {
      keysRecord.encrypt();
    } catch (UnsupportedEncodingException e) {
      keyUploadDelegate.onKeyUploadFailed(e);
      return;
    } catch (CryptoException e) {
      keyUploadDelegate.onKeyUploadFailed(e);
      return;
    }
    request.put(keysRecord);
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
    Log.i(LOG_TAG, "Running next stage " + next);
    try {
      nextStage.execute(this);
    } catch (Exception ex) {
      Log.w(LOG_TAG, "Caught exception " + ex + " running stage " + next);
      this.abort(ex, "Uncaught exception in stage.");
    }
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

  /**
   * Stop this sync and start again.
   * @throws AlreadySyncingException
   */
  protected void restart() throws AlreadySyncingException {
    this.currentState = GlobalSyncStage.Stage.idle;
    // TODO: respect backoff.
    this.start();
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

  public void abort(Exception e, String reason) {
    Log.w(LOG_TAG, "Aborting sync: " + reason);
    e.printStackTrace();
    this.callback.handleError(this, e);
  }

  public void handleHTTPError(SyncStorageResponse response, String reason) {
    // TODO: handling of 50x (backoff), 401 (node reassignment or auth error).
    // Fall back to aborting.
    Log.w(LOG_TAG, "Aborting sync due to HTTP " + response.getStatusCode());
    this.abort(new HTTPFailureException(response), reason);
  }






  public void processMetaGlobal(MetaGlobal global) {
    Long storageVersion = global.getStorageVersion();
    if (storageVersion < STORAGE_VERSION) {
      // Outdated server.
      this.freshStart();
      return;
    }
    if (storageVersion > STORAGE_VERSION) {
      // Outdated client!
      this.requiresUpgrade();
      return;
    }
    String remoteSyncID = global.getSyncID();
    if (remoteSyncID == null) {
      // Corrupt meta/global.
      this.freshStart();
    }
    String localSyncID = this.getSyncID();
    if (!remoteSyncID.equals(localSyncID)) {
      // Sync ID has changed. Reset timestamps and fetch new keys.
      // TODO TODO TODO
    }
    try {
      this.advance();
    } catch (NoSuchStageException e) {
      // TODO: shouldn't happen.
    }
  }

  private String getSyncID() {
    return syncID;
  }
  public void processMissingMetaGlobal(MetaGlobal global) {
    this.freshStart();
  }

  /**
   * Do a fresh start then quietly finish the sync, starting another.
   */
  protected void freshStart() {
    final GlobalSession globalSession = this;
    this.freshStart(this, new FreshStartDelegate() {

      @Override
      public void onFreshStartFailed(Exception e) {
        globalSession.abort(e, "Fresh start failed.");
      }

      @Override
      public void onFreshStart() {
        try {
          globalSession.restart();
        } catch (Exception e) {
          Log.w(LOG_TAG, "Got exception when restarting sync after freshStart.", e);
          globalSession.abort(e, "Got exception after freshStart.");
        }
      }
    });
  }

  /**
   * Clean the server, aborting the current sync.
   */
  protected void freshStart(final GlobalSession session, final FreshStartDelegate freshStartDelegate) {

    final String newSyncID   = session.generateSyncID();
    final String metaURL     = session.getMetaURL();
    final String credentials = session.credentials();

    this.wipeServer(session, new WipeServerDelegate() {

      @Override
      public void onWiped(long timestamp) {
        session.resetClient();
        session.collectionKeys.clear();      // TODO: make sure we clear our keys timestamp.

        metaGlobal = new MetaGlobal(metaURL, credentials);
        metaGlobal.setSyncID(newSyncID);
        metaGlobal.setStorageVersion(STORAGE_VERSION);

        // It would be good to set the X-If-Unmodified-Since header to `timestamp`
        // for this PUT to ensure at least some level of transactionality.
        // Unfortunately, the servers don't support it after a wipe right now
        // (bug 693893), so we're going to defer this until bug 692700.
        metaGlobal.upload(new MetaGlobalDelegate() {

          @Override
          public void handleSuccess(MetaGlobal global) {
            session.metaGlobal = global;
            Log.i(LOG_TAG, "New meta/global uploaded with sync ID " + newSyncID);

            // Generate and upload new keys.
            try {
              session.uploadKeys(CollectionKeys.generateCollectionKeys().asCryptoRecord(), new KeyUploadDelegate() {
                @Override
                public void onKeysUploaded() {
                  // Now we can download them.
                  freshStartDelegate.onFreshStart();
                }

                @Override
                public void onKeyUploadFailed(Exception e) {
                  Log.e(LOG_TAG, "Got exception uploading new keys.", e);
                  freshStartDelegate.onFreshStartFailed(e);
                }
              });
            } catch (NoCollectionKeysSetException e) {
              Log.e(LOG_TAG, "Got exception generating new keys.", e);
              freshStartDelegate.onFreshStartFailed(e);
            } catch (CryptoException e) {
              Log.e(LOG_TAG, "Got exception generating new keys.", e);
              freshStartDelegate.onFreshStartFailed(e);
            }
          }

          @Override
          public void handleMissing(MetaGlobal global) {
            // Shouldn't happen.
            Log.w(LOG_TAG, "Got 'missing' response uploading new meta/global.");
            freshStartDelegate.onFreshStartFailed(new Exception("meta/global missing"));
          }

          @Override
          public void handleFailure(SyncStorageResponse response) {
            // TODO: respect backoffs etc.
            Log.w(LOG_TAG, "Got failure " + response.getStatusCode() + " uploading new meta/global.");
            freshStartDelegate.onFreshStartFailed(new HTTPFailureException(response));
          }

          @Override
          public void handleError(Exception e) {
            Log.w(LOG_TAG, "Got error uploading new meta/global.", e);
            freshStartDelegate.onFreshStartFailed(e);
          }
        });
      }

      @Override
      public void onWipeFailed(Exception e) {
        Log.w(LOG_TAG, "Wipe failed.");
        freshStartDelegate.onFreshStartFailed(e);
      }
    });

  }

  private void wipeServer(final CredentialsSource credentials, final WipeServerDelegate wipeDelegate) {
    SyncStorageRequest request;
    try {
      request = new SyncStorageRequest(this.storageURL(false));
    } catch (URISyntaxException ex) {
      Log.w(LOG_TAG, "Invalid URI in wipeServer.");
      wipeDelegate.onWipeFailed(ex);
      return;
    }

    request.delegate = new SyncStorageRequestDelegate() {

      @Override
      public String ifUnmodifiedSince() {
        return null;
      }

      @Override
      public void handleRequestSuccess(SyncStorageResponse response) {
        wipeDelegate.onWiped(response.weaveTimestamp());
      }

      @Override
      public void handleRequestFailure(SyncStorageResponse response) {
        Log.w(LOG_TAG, "Got request failure " + response.getStatusCode() + " in wipeServer.");
        wipeDelegate.onWipeFailed(new HTTPFailureException(response));
      }

      @Override
      public void handleRequestError(Exception ex) {
        Log.w(LOG_TAG, "Got exception in wipeServer.", ex);
        wipeDelegate.onWipeFailed(ex);
      }

      @Override
      public String credentials() {
        return credentials.credentials();
      }
    };
    request.delete();
  }

  private void resetClient() {
    // TODO Auto-generated method stub

  }

  private String generateSyncID() {
    syncID = Utils.generateGuid();
    return syncID;
  }

  /**
   * Suggest that your Sync client needs to be upgraded to work
   * with this server.
   */
  public void requiresUpgrade() {
    Log.i(LOG_TAG, "Client outdated storage version; requires update.");
    // TODO: notify UI.
  }
}
