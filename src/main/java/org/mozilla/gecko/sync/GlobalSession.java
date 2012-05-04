/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.delegates.ClientsDataDelegate;
import org.mozilla.gecko.sync.delegates.FreshStartDelegate;
import org.mozilla.gecko.sync.delegates.GlobalSessionCallback;
import org.mozilla.gecko.sync.delegates.InfoCollectionsDelegate;
import org.mozilla.gecko.sync.delegates.KeyUploadDelegate;
import org.mozilla.gecko.sync.delegates.MetaGlobalDelegate;
import org.mozilla.gecko.sync.delegates.WipeServerDelegate;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.HttpResponseObserver;
import org.mozilla.gecko.sync.net.SyncResponse;
import org.mozilla.gecko.sync.net.SyncStorageRecordRequest;
import org.mozilla.gecko.sync.net.SyncStorageRequest;
import org.mozilla.gecko.sync.net.SyncStorageRequestDelegate;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.mozilla.gecko.sync.stage.AndroidBrowserBookmarksServerSyncStage;
import org.mozilla.gecko.sync.stage.AndroidBrowserHistoryServerSyncStage;
import org.mozilla.gecko.sync.stage.CheckPreconditionsStage;
import org.mozilla.gecko.sync.stage.CompletedStage;
import org.mozilla.gecko.sync.stage.EnsureClusterURLStage;
import org.mozilla.gecko.sync.stage.EnsureCrypto5KeysStage;
import org.mozilla.gecko.sync.stage.FennecTabsServerSyncStage;
import org.mozilla.gecko.sync.stage.FetchInfoCollectionsStage;
import org.mozilla.gecko.sync.stage.FetchMetaGlobalStage;
import org.mozilla.gecko.sync.stage.FormHistoryServerSyncStage;
import org.mozilla.gecko.sync.stage.GlobalSyncStage;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;
import org.mozilla.gecko.sync.stage.NoSuchStageException;
import org.mozilla.gecko.sync.stage.PasswordsServerSyncStage;
import org.mozilla.gecko.sync.stage.SyncClientsEngineStage;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import ch.boye.httpclientandroidlib.HttpResponse;

public class GlobalSession implements CredentialsSource, PrefsSource, HttpResponseObserver {
  private static final String LOG_TAG = "GlobalSession";

  public static final String API_VERSION   = "1.1";
  public static final long STORAGE_VERSION = 5;
  public static final int  BOOKMARKS_ENGINE_VERSION = 2;
  public static final int  CLIENTS_ENGINE_VERSION = 1;
  public static final int  FORMS_ENGINE_VERSION = 1;
  public static final int  HISTORY_ENGINE_VERSION = 1;
  public static final int  PASSWORDS_ENGINE_VERSION = 1;
  public static final int  TABS_ENGINE_VERSION = 1;

  public SyncConfiguration config = null;

  protected Map<Stage, GlobalSyncStage> stages;
  public Stage currentState = Stage.idle;

  public final GlobalSessionCallback callback;
  private Context context;
  private ClientsDataDelegate clientsDelegate;

  /*
   * Key accessors.
   */
  public KeyBundle keyBundleForCollection(String collection) throws NoCollectionKeysSetException {
    return config.getCollectionKeys().keyBundleForCollection(collection);
  }

  /*
   * Config passthrough for convenience.
   */
  @Override
  public String credentials() {
    return config.credentials();
  }

  public URI wboURI(String collection, String id) throws URISyntaxException {
    return config.wboURI(collection, id);
  }

  /*
   * Validators.
   */
  private static boolean isInvalidString(String s) {
    return s == null ||
           s.trim().length() == 0;
  }

  private static boolean anyInvalidStrings(String s, String...strings) {
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

  public GlobalSession(String userAPI,
                       String serverURL,
                       String username,
                       String password,
                       String prefsPath,
                       KeyBundle syncKeyBundle,
                       GlobalSessionCallback callback,
                       Context context,
                       Bundle extras,
                       ClientsDataDelegate clientsDelegate)
                           throws SyncConfigurationException, IllegalArgumentException, IOException, ParseException, NonObjectJSONException {
    if (callback == null) {
      throw new IllegalArgumentException("Must provide a callback to GlobalSession constructor.");
    }

    if (anyInvalidStrings(username, password)) {
      throw new SyncConfigurationException();
    }

    Logger.info(LOG_TAG, "GlobalSession initialized with bundle " + extras);
    URI serverURI;
    try {
      serverURI = (serverURL == null) ? null : new URI(serverURL);
    } catch (URISyntaxException e) {
      throw new SyncConfigurationException();
    }

    if (syncKeyBundle == null ||
        syncKeyBundle.getEncryptionKey() == null ||
        syncKeyBundle.getHMACKey() == null) {
      throw new SyncConfigurationException();
    }

    this.callback        = callback;
    this.context         = context;
    this.clientsDelegate = clientsDelegate;

    config = new SyncConfiguration(prefsPath, this);
    config.userAPI       = userAPI;
    config.serverURL     = serverURI;
    config.username      = username;
    config.password      = password;
    config.syncKeyBundle = syncKeyBundle;

    registerCommands();
    prepareStages();

    // TODO: data-driven plan for the sync, referring to prepareStages.
  }

  protected void registerCommands() {
    CommandProcessor processor = CommandProcessor.getProcessor();

    processor.registerCommand("resetEngine", new CommandRunner() {
      @Override
      public void executeCommand(List<String> args) {
        HashSet<String> names = new HashSet<String>();
        names.add(args.get(0));
        resetStagesByName(names);
      }
    });

    processor.registerCommand("resetAll", new CommandRunner() {
      @Override
      public void executeCommand(List<String> args) {
        resetAllStages();
      }
    });

    processor.registerCommand("wipeEngine", new CommandRunner() {
      @Override
      public void executeCommand(List<String> args) {
        HashSet<String> names = new HashSet<String>();
        names.add(args.get(0));
        wipeStagesByName(names);
      }
    });

    processor.registerCommand("wipeAll", new CommandRunner() {
      @Override
      public void executeCommand(List<String> args) {
        wipeAllStages();
      }
    });

    processor.registerCommand("displayURI", new CommandRunner() {
      @Override
      public void executeCommand(List<String> args) {
        CommandProcessor.getProcessor().displayURI(args, getContext());
      }
    });
  }

  protected void prepareStages() {
    HashMap<Stage, GlobalSyncStage> stages = new HashMap<Stage, GlobalSyncStage>();

    stages.put(Stage.checkPreconditions,      new CheckPreconditionsStage(this));
    stages.put(Stage.ensureClusterURL,        new EnsureClusterURLStage(this));
    stages.put(Stage.fetchInfoCollections,    new FetchInfoCollectionsStage(this));
    stages.put(Stage.fetchMetaGlobal,         new FetchMetaGlobalStage(this));
    stages.put(Stage.ensureKeysStage,         new EnsureCrypto5KeysStage(this));
    stages.put(Stage.syncClientsEngine,       new SyncClientsEngineStage(this));

    stages.put(Stage.syncTabs,                new FennecTabsServerSyncStage(this));
    stages.put(Stage.syncPasswords,           new PasswordsServerSyncStage(this));
    stages.put(Stage.syncBookmarks,           new AndroidBrowserBookmarksServerSyncStage(this));
    stages.put(Stage.syncHistory,             new AndroidBrowserHistoryServerSyncStage(this));
    stages.put(Stage.syncFormHistory,         new FormHistoryServerSyncStage(this));

    stages.put(Stage.completed,               new CompletedStage(this));

    this.stages = Collections.unmodifiableMap(stages);
  }

  public GlobalSyncStage getSyncStageByName(String name) throws NoSuchStageException {
    return getSyncStageByName(Stage.byName(name));
  }

  public GlobalSyncStage getSyncStageByName(Stage next) throws NoSuchStageException {
    GlobalSyncStage stage = stages.get(next);
    if (stage == null) {
      throw new NoSuchStageException(next);
    }
    return stage;
  }

  public Collection<GlobalSyncStage> getSyncStagesByEnum(Collection<Stage> enums) {
    ArrayList<GlobalSyncStage> out = new ArrayList<GlobalSyncStage>();
    for (Stage name : enums) {
      try {
        GlobalSyncStage stage = this.getSyncStageByName(name);
        out.add(stage);
      } catch (NoSuchStageException e) {
        Logger.warn(LOG_TAG, "Unable to find stage with name " + name);
      }
    }
    return out;
  }

  public Collection<GlobalSyncStage> getSyncStagesByName(Collection<String> names) {
    ArrayList<GlobalSyncStage> out = new ArrayList<GlobalSyncStage>();
    for (String name : names) {
      try {
        GlobalSyncStage stage = this.getSyncStageByName(name);
        out.add(stage);
      } catch (NoSuchStageException e) {
        Logger.warn(LOG_TAG, "Unable to find stage with name " + name);
      }
    }
    return out;
  }

  /**
   * Advance and loop around the stages of a sync.
   * @param current
   * @return
   *        The next stage to execute.
   */
  public static Stage nextStage(Stage current) {
    int index = current.ordinal() + 1;
    int max   = Stage.completed.ordinal() + 1;
    return Stage.values()[index % max];
  }

  /**
   * Move to the next stage in the syncing process.
   */
  public void advance() {
    // If we have a backoff, request a backoff and don't advance to next stage.
    long existingBackoff = largestBackoffObserved.get();
    if (existingBackoff > 0) {
      this.abort(null, "Aborting sync because of backoff of " + existingBackoff + " milliseconds.");
      return;
    }

    this.callback.handleStageCompleted(this.currentState, this);
    Stage next = nextStage(this.currentState);
    GlobalSyncStage nextStage;
    try {
      nextStage = this.getSyncStageByName(next);
    } catch (NoSuchStageException e) {
      this.abort(e, "No such stage " + next);
      return;
    }
    this.currentState = next;
    Logger.info(LOG_TAG, "Running next stage " + next + " (" + nextStage + ")...");
    try {
      nextStage.execute();
    } catch (Exception ex) {
      Logger.warn(LOG_TAG, "Caught exception " + ex + " running stage " + next);
      this.abort(ex, "Uncaught exception in stage.");
      return;
    }
  }

  /*
   * PrefsSource methods.
   */
  @Override
  public SharedPreferences getPrefs(String name, int mode) {
    return this.getContext().getSharedPreferences(name, mode);
  }

  public Context getContext() {
    return this.context;
  }

  /**
   * Begin a sync.
   * <p>
   * The caller is responsible for:
   * <ul>
   * <li>Verifying that any backoffs/minimum next sync requests are respected.</li>
   * <li>Ensuring that the device is online.</li>
   * <li>Ensuring that dependencies are ready.</li>
   * </ul>
   *
   * @throws AlreadySyncingException
   */
  public void start() throws AlreadySyncingException {
    if (this.currentState != GlobalSyncStage.Stage.idle) {
      throw new AlreadySyncingException(this.currentState);
    }
    installAsHttpResponseObserver(); // Uninstalled by completeSync or abort.
    this.advance();
  }

  /**
   * Stop this sync and start again.
   * @throws AlreadySyncingException
   */
  protected void restart() throws AlreadySyncingException {
    this.currentState = GlobalSyncStage.Stage.idle;
    if (callback.shouldBackOff()) {
      this.callback.handleAborted(this, "Told to back off.");
      return;
    }
    this.start();
  }

  public void completeSync() {
    uninstallAsHttpResponseObserver();
    this.currentState = GlobalSyncStage.Stage.idle;
    this.callback.handleSuccess(this);
  }

  public void abort(Exception e, String reason) {
    Logger.warn(LOG_TAG, "Aborting sync: " + reason, e);
    uninstallAsHttpResponseObserver();
    long existingBackoff = largestBackoffObserved.get();
    if (existingBackoff > 0) {
      callback.requestBackoff(existingBackoff);
    }
    this.callback.handleError(this, e);
  }

  public void handleHTTPError(SyncStorageResponse response, String reason) {
    // TODO: handling of 50x (backoff), 401 (node reassignment or auth error).
    // Fall back to aborting.
    Logger.warn(LOG_TAG, "Aborting sync due to HTTP " + response.getStatusCode());
    this.interpretHTTPFailure(response.httpResponse());
    this.abort(new HTTPFailureException(response), reason);
  }

  /**
   * Perform appropriate backoff etc. extraction.
   */
  public void interpretHTTPFailure(HttpResponse response) {
    // TODO: handle permanent rejection.
    long responseBackoff = (new SyncResponse(response)).totalBackoffInMilliseconds();
    if (responseBackoff > 0) {
      callback.requestBackoff(responseBackoff);
    }

    if (response.getStatusLine() != null && response.getStatusLine().getStatusCode() == 401) {
      /*
       * Alert our callback we have a 401 on a cluster URL. This GlobalSession
       * will fail, but the next one will fetch a new cluster URL and will
       * distinguish between "node reassignment" and "user password changed".
       */
      callback.informUnauthorizedResponse(this, config.getClusterURL());
    }
  }

  public void fetchInfoCollections(InfoCollectionsDelegate callback) throws URISyntaxException {
    if (this.config.infoCollections == null) {
      this.config.infoCollections = new InfoCollections(config.infoURL(), credentials());
    }
    this.config.infoCollections.fetch(callback);
  }

  /**
   * Upload new crypto/keys.
   *
   * @param keys
   *          new keys.
   * @param keyUploadDelegate
   *          a delegate.
   */
  public void uploadKeys(final CollectionKeys keys,
                         final KeyUploadDelegate keyUploadDelegate) {
    SyncStorageRecordRequest request;
    final GlobalSession self = this;
    try {
      request = new SyncStorageRecordRequest(this.config.keysURI());
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
        BaseResource.consumeEntity(response); // We don't need the response at all.
        keyUploadDelegate.onKeysUploaded();
      }

      @Override
      public void handleRequestFailure(SyncStorageResponse response) {
        self.interpretHTTPFailure(response.httpResponse());
        BaseResource.consumeEntity(response); // The exception thrown should not need the body of the response.
        keyUploadDelegate.onKeyUploadFailed(new HTTPFailureException(response));
      }

      @Override
      public void handleRequestError(Exception ex) {
        keyUploadDelegate.onKeyUploadFailed(ex);
      }

      @Override
      public String credentials() {
        return self.credentials();
      }
    };

    CryptoRecord keysRecord;
    try {
      keysRecord = keys.asCryptoRecord();
      keysRecord.setKeyBundle(config.syncKeyBundle);
      keysRecord.encrypt();
    } catch (UnsupportedEncodingException e) {
      keyUploadDelegate.onKeyUploadFailed(e);
      return;
    } catch (CryptoException e) {
      keyUploadDelegate.onKeyUploadFailed(e);
      return;
    } catch (NoCollectionKeysSetException e) {
      // Should not occur.
      keyUploadDelegate.onKeyUploadFailed(e);
      return;
    }

    request.put(keysRecord);
  }


  /*
   * meta/global callbacks.
   */
  public void processMetaGlobal(MetaGlobal global) {
    config.metaGlobal = global;

    Long storageVersion = global.getStorageVersion();
    if (storageVersion == null) {
      Logger.warn(LOG_TAG, "Malformed remote meta/global: could not retrieve remote storage version.");
      freshStart();
      return;
    }
    if (storageVersion < STORAGE_VERSION) {
      Logger.warn(LOG_TAG, "Outdated server: reported " +
          "remote storage version " + storageVersion + " < " +
          "local storage version " + STORAGE_VERSION);
      freshStart();
      return;
    }
    if (storageVersion > STORAGE_VERSION) {
      Logger.warn(LOG_TAG, "Outdated client: reported " +
          "remote storage version " + storageVersion + " > " +
          "local storage version " + STORAGE_VERSION);
      requiresUpgrade();
      return;
    }
    String remoteSyncID = global.getSyncID();
    if (remoteSyncID == null) {
      Logger.warn(LOG_TAG, "Malformed remote meta/global: could not retrieve remote syncID.");
      freshStart();
      return;
    }
    String localSyncID = config.syncID;
    if (!remoteSyncID.equals(localSyncID)) {
      Logger.warn(LOG_TAG, "Remote syncID different from local syncID: resetting client and assuming remote syncID.");
      resetAllStages();
      config.purgeCryptoKeys();
      config.syncID = remoteSyncID;
    }
    // Persist enabled engine names.
    config.enabledEngineNames = global.getEnabledEngineNames();
    if (config.enabledEngineNames == null) {
      Logger.warn(LOG_TAG, "meta/global reported no enabled engine names!");
    } else {
      Logger.debug(LOG_TAG, "Persisting enabled engine names '" +
          Utils.toCommaSeparatedString(config.enabledEngineNames) + "' from meta/global.");
    }
    config.persistToPrefs();
    advance();
  }

  public void processMissingMetaGlobal(MetaGlobal global) {
    freshStart();
  }

  /**
   * Do a fresh start then quietly finish the sync, starting another.
   */
  public void freshStart() {
    final GlobalSession globalSession = this;
    freshStart(this, new FreshStartDelegate() {

      @Override
      public void onFreshStartFailed(Exception e) {
        globalSession.abort(e, "Fresh start failed.");
      }

      @Override
      public void onFreshStart() {
        try {
          Logger.warn(LOG_TAG, "Fresh start succeeded; restarting global session.");
          globalSession.config.persistToPrefs();
          globalSession.restart();
        } catch (Exception e) {
          Logger.warn(LOG_TAG, "Got exception when restarting sync after freshStart.", e);
          globalSession.abort(e, "Got exception after freshStart.");
        }
      }
    });
  }

  /**
   * Clean the server, aborting the current sync.
   */
  protected void freshStart(final GlobalSession session, final FreshStartDelegate freshStartDelegate) {
    final MetaGlobal mg = generateNewMetaGlobal();

    wipeServer(session, new WipeServerDelegate() {

      @Override
      public void onWiped(long timestamp) {
        session.resetAllStages();
        session.config.purgeCryptoKeys();
        session.config.purgeMetaGlobal();
        session.config.persistToPrefs();

        Logger.info(LOG_TAG, "Uploading new meta/global with sync ID " + mg.syncID);

        // It would be good to set the X-If-Unmodified-Since header to `timestamp`
        // for this PUT to ensure at least some level of transactionality.
        // Unfortunately, the servers don't support it after a wipe right now
        // (bug 693893), so we're going to defer this until bug 692700.
        mg.upload(new MetaGlobalDelegate() {
          @Override
          public void handleSuccess(MetaGlobal uploadedGlobal, SyncStorageResponse uploadResponse) {
            Logger.info(LOG_TAG, "New meta/global uploaded with sync ID " + uploadedGlobal.syncID);
            // Now we can download the new meta/global. We re-download to
            // shorten the window in which two clients can race while
            // uploading meta/global; with luck, both will end up downloading
            // the same meta/global record.

            mg.fetch(new MetaGlobalDelegate() {
              @Override
              public void handleSuccess(MetaGlobal downloadedGlobal, SyncStorageResponse downloadResponse) {
                Logger.info(LOG_TAG, "New meta/global downloaded with sync ID " + downloadedGlobal.syncID);
                session.config.metaGlobal = downloadedGlobal;

                // Generate and upload new keys.
                try {
                  session.uploadKeys(generateNewCryptoKeys(), new KeyUploadDelegate() {
                    @Override
                    public void onKeysUploaded() {
                      // Now we can download the new keys. We re-download to
                      // shorten the window in which two clients can race while
                      // uploading keys; with luck, both will end up downloading
                      // the same keys.
                      freshStartDelegate.onFreshStart();
                    }

                    @Override
                    public void onKeyUploadFailed(Exception e) {
                      Log.e(LOG_TAG, "Got exception uploading new keys.", e);
                      freshStartDelegate.onFreshStartFailed(e);
                    }
                  });
                } catch (CryptoException e) {
                  Log.e(LOG_TAG, "Got exception generating new keys.", e);
                  freshStartDelegate.onFreshStartFailed(e);
                }
              }

              @Override
              public void handleMissing(MetaGlobal global, SyncStorageResponse response) {
                // Shouldn't happen.
                Logger.warn(LOG_TAG, "Got 'missing' response downloading new meta/global.");
                freshStartDelegate.onFreshStartFailed(new Exception("meta/global missing while downloading."));
              }

              @Override
              public void handleFailure(SyncStorageResponse response) {
                Logger.warn(LOG_TAG, "Got failure " + response.getStatusCode() + " downloading new meta/global.");
                session.interpretHTTPFailure(response.httpResponse());
                freshStartDelegate.onFreshStartFailed(new HTTPFailureException(response));
              }

              @Override
              public void handleError(Exception e) {
                Logger.warn(LOG_TAG, "Got error downloading new meta/global.", e);
                freshStartDelegate.onFreshStartFailed(e);
              }
            });
          }

          @Override
          public void handleMissing(MetaGlobal global, SyncStorageResponse response) {
            // Shouldn't happen.
            Logger.warn(LOG_TAG, "Got 'missing' response uploading new meta/global.");
            freshStartDelegate.onFreshStartFailed(new Exception("meta/global missing while uploading."));
          }

          @Override
          public void handleFailure(SyncStorageResponse response) {
            Logger.warn(LOG_TAG, "Got failure " + response.getStatusCode() + " uploading new meta/global.");
            session.interpretHTTPFailure(response.httpResponse());
            freshStartDelegate.onFreshStartFailed(new HTTPFailureException(response));
          }

          @Override
          public void handleError(Exception e) {
            Logger.warn(LOG_TAG, "Got error uploading new meta/global.", e);
            freshStartDelegate.onFreshStartFailed(e);
          }
        });
      }

      @Override
      public void onWipeFailed(Exception e) {
        Logger.warn(LOG_TAG, "Wipe failed.");
        freshStartDelegate.onFreshStartFailed(e);
      }
    });
  }

  // Note that we do not yet implement wipeRemote: it's only necessary for
  // first sync options.
  // -- reset local stages, wipe server for each stage *except* clients
  //    (stages only, not whole server!), send wipeEngine commands to each client.
  //
  // Similarly for startOver (because we don't receive that notification).
  // -- remove client data from server, reset local stages, clear keys, reset
  //    backoff, clear all prefs, discard credentials.
  //
  // Change passphrase: wipe entire server, reset client to force upload, sync.
  //
  // When an engine is disabled: wipe its collections on the server, reupload
  // meta/global.
  //
  // On syncing each stage: if server has engine version 0 or old, wipe server,
  // reset client to prompt reupload.
  // If sync ID mismatch: take that syncID and reset client.

  protected void wipeServer(final CredentialsSource credentials, final WipeServerDelegate wipeDelegate) {
    SyncStorageRequest request;
    final GlobalSession self = this;

    try {
      request = new SyncStorageRequest(config.storageURL(false));
    } catch (URISyntaxException ex) {
      Logger.warn(LOG_TAG, "Invalid URI in wipeServer.");
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
        BaseResource.consumeEntity(response);
        wipeDelegate.onWiped(response.normalizedWeaveTimestamp());
      }

      @Override
      public void handleRequestFailure(SyncStorageResponse response) {
        Logger.warn(LOG_TAG, "Got request failure " + response.getStatusCode() + " in wipeServer.");
        // Process HTTP failures here to pick up backoffs, etc.
        self.interpretHTTPFailure(response.httpResponse());
        BaseResource.consumeEntity(response); // The exception thrown should not need the body of the response.
        wipeDelegate.onWipeFailed(new HTTPFailureException(response));
      }

      @Override
      public void handleRequestError(Exception ex) {
        Logger.warn(LOG_TAG, "Got exception in wipeServer.", ex);
        wipeDelegate.onWipeFailed(ex);
      }

      @Override
      public String credentials() {
        return credentials.credentials();
      }
    };
    request.delete();
  }

  public void wipeAllStages() {
    Logger.info(LOG_TAG, "Wiping all stages.");
    // Includes "clients".
    this.wipeStagesByEnum(Stage.getNamedStages());
  }

  public static void wipeStages(Collection<GlobalSyncStage> stages) {
    for (GlobalSyncStage stage : stages) {
      try {
        Logger.info(LOG_TAG, "Wiping " + stage);
        stage.wipeLocal();
      } catch (Exception e) {
        Logger.error(LOG_TAG, "Ignoring wipe failure for stage " + stage, e);
      }
    }
  }

  public void wipeStagesByEnum(Collection<Stage> stages) {
    GlobalSession.wipeStages(this.getSyncStagesByEnum(stages));
  }

  public void wipeStagesByName(Collection<String> names) {
    GlobalSession.wipeStages(this.getSyncStagesByName(names));
  }

  public void resetAllStages() {
    Logger.info(LOG_TAG, "Resetting all stages.");
    // Includes "clients".
    this.resetStagesByEnum(Stage.getNamedStages());
  }

  public static void resetStages(Collection<GlobalSyncStage> stages) {
    for (GlobalSyncStage stage : stages) {
      try {
        Logger.info(LOG_TAG, "Resetting " + stage);
        stage.resetLocal();
      } catch (Exception e) {
        Logger.error(LOG_TAG, "Ignoring reset failure for stage " + stage, e);
      }
    }
  }

  public void resetStagesByEnum(Collection<Stage> stages) {
    GlobalSession.resetStages(this.getSyncStagesByEnum(stages));
  }

  public void resetStagesByName(Collection<String> names) {
    Collection<GlobalSyncStage> stages = new ArrayList<GlobalSyncStage>();
    for (String name : names) {
      try {
        GlobalSyncStage stage = this.getSyncStageByName(name);
        stages.add(stage);
      } catch (NoSuchStageException e) {
        Logger.warn(LOG_TAG, "Cannot reset stage " + name + ": no such stage.");
      }
    }
    GlobalSession.resetStages(stages);
  }

  /**
   * Engines to include in a fresh meta/global record.
   * <p>
   * Returns either the persisted engine names (perhaps we have been node
   * re-assigned and are initializing a clean server: we want to upload the
   * persisted engine names so that we don't accidentally disable engines that
   * Android Sync doesn't recognize), or the set of engines names that Android
   * Sync implements.
   *
   * @return set of engine names.
   */
  protected Set<String> enabledEngineNames() {
    if (config.enabledEngineNames != null) {
      return config.enabledEngineNames;
    }
    Set<String> engineNames = new HashSet<String>();
    for (Stage stage : Stage.getNamedStages()) {
      engineNames.add(stage.getRepositoryName());
    }
    return engineNames;
  }

  /**
   * Generate fresh crypto/keys collection.
   * @return crypto/keys collection.
   * @throws CryptoException
   */
  public CollectionKeys generateNewCryptoKeys() throws CryptoException {
    return CollectionKeys.generateCollectionKeys();
  }

  /**
   * Generate a fresh meta/global record.
   * @return meta/global record.
   */
  public MetaGlobal generateNewMetaGlobal() {
    final String newSyncID   = Utils.generateGuid();
    final String metaURL     = this.config.metaURL();
    final String credentials = this.credentials();

    ExtendedJSONObject engines = new ExtendedJSONObject();
    for (String engineName : enabledEngineNames()) {
      EngineSettings engineSettings = null;
      try {
        GlobalSyncStage globalStage = this.getSyncStageByName(engineName);
        Integer version = globalStage.getStorageVersion();
        if (version == null) {
          continue; // Don't want this stage to be included in meta/global.
        }
        engineSettings = new EngineSettings(Utils.generateGuid(), version.intValue());
      } catch (NoSuchStageException e) {
        // No trouble; Android Sync might not recognize this engine yet.
        // By default, version 0.  Other clients will see the 0 version and reset/wipe accordingly.
        engineSettings = new EngineSettings(Utils.generateGuid(), 0);
      }
      engines.put(engineName, engineSettings.toJSONObject());
    }

    MetaGlobal metaGlobal = new MetaGlobal(metaURL, credentials);
    metaGlobal.setSyncID(newSyncID);
    metaGlobal.setStorageVersion(STORAGE_VERSION);
    metaGlobal.setEngines(engines);

    return metaGlobal;
  }

  /**
   * Suggest that your Sync client needs to be upgraded to work
   * with this server.
   */
  public void requiresUpgrade() {
    Logger.info(LOG_TAG, "Client outdated storage version; requires update.");
    // TODO: notify UI.
    this.abort(null, "Requires upgrade");
  }

  /**
   * If meta/global is missing or malformed, throws a MetaGlobalException.
   * Otherwise, returns true if there is an entry for this engine in the
   * meta/global "engines" object.
   *
   * @param engineName the name to check (e.g., "bookmarks").
   * @param engineSettings
   *        if non-null, verify that the server engine settings are congruent
   *        with this, throwing the appropriate MetaGlobalException if not.
   * @return
   *        true if the engine with the provided name is present in the
   *        meta/global "engines" object, and verification passed.
   *
   * @throws MetaGlobalException
   */
  public boolean engineIsEnabled(String engineName, EngineSettings engineSettings) throws MetaGlobalException {
    if (this.config.metaGlobal == null) {
      throw new MetaGlobalNotSetException();
    }
    if (this.config.metaGlobal.engines == null) {
      throw new MetaGlobalMissingEnginesException();
    }
    ExtendedJSONObject engineEntry;
    try {
      engineEntry = this.config.metaGlobal.engines.getObject(engineName);
    } catch (NonObjectJSONException e) {
      Logger.error(LOG_TAG, "Engine field for " + engineName + " in meta/global is not an object.");
      throw new MetaGlobalMissingEnginesException();
    }

    if (engineEntry == null) {
      Logger.debug(LOG_TAG, "Engine " + engineName + " not enabled: no meta/global entry.");
      return false;
    }

    if (engineSettings != null) {
      MetaGlobal.verifyEngineSettings(engineEntry, engineSettings);
    }
    return true;
  }

  public ClientsDataDelegate getClientsDelegate() {
    return this.clientsDelegate;
  }

  /**
   * The longest backoff observed to date; -1 means no backoff observed.
   */
  protected final AtomicLong largestBackoffObserved = new AtomicLong(-1);

  /**
   * Reset any observed backoff and start observing HTTP responses for backoff
   * requests.
   */
  protected void installAsHttpResponseObserver() {
    Logger.debug(LOG_TAG, "Installing " + this + " as BaseResource HttpResponseObserver.");
    BaseResource.setHttpResponseObserver(this);
    largestBackoffObserved.set(-1);
  }

  /**
   * Stop observing HttpResponses for backoff requests.
   */
  protected void uninstallAsHttpResponseObserver() {
    Logger.debug(LOG_TAG, "Uninstalling " + this + " as BaseResource HttpResponseObserver.");
    BaseResource.setHttpResponseObserver(null);
  }

  /**
   * Observe all HTTP response for backoff requests on all status codes, not just errors.
   */
  @Override
  public void observeHttpResponse(HttpResponse response) {
    long responseBackoff = (new SyncResponse(response)).totalBackoffInMilliseconds(); // TODO: don't allocate object?
    if (responseBackoff <= 0) {
      return;
    }

    Logger.debug(LOG_TAG, "Observed " + responseBackoff + " millisecond backoff request.");
    while (true) {
      long existingBackoff = largestBackoffObserved.get();
      if (existingBackoff >= responseBackoff) {
        return;
      }
      if (largestBackoffObserved.compareAndSet(existingBackoff, responseBackoff)) {
        return;
      }
    }
  }
}
