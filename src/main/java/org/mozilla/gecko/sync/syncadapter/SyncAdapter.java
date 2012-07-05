/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.syncadapter;

import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.sync.AlreadySyncingException;
import org.mozilla.gecko.sync.GlobalConstants;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.SyncException;
import org.mozilla.gecko.sync.ThreadPool;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.delegates.ClientsDataDelegate;
import org.mozilla.gecko.sync.delegates.GlobalSessionCallback;
import org.mozilla.gecko.sync.net.ConnectionMonitorThread;
import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.setup.SyncAccounts;
import org.mozilla.gecko.sync.setup.SyncAccounts.SyncAccountParameters;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SyncResult;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;

public class SyncAdapter extends AbstractThreadedSyncAdapter implements GlobalSessionCallback, ClientsDataDelegate {
  private static final String  LOG_TAG = "SyncAdapter";

  private static final int     BACKOFF_PAD_SECONDS = 5;
  public  static final int     MULTI_DEVICE_INTERVAL_MILLISECONDS = 5 * 60 * 1000;         // 5 minutes.
  public  static final int     SINGLE_DEVICE_INTERVAL_MILLISECONDS = 24 * 60 * 60 * 1000;  // 24 hours.

  private final Context        mContext;

  public SyncAdapter(Context context, boolean autoInitialize) {
    super(context, autoInitialize);
    mContext = context;
  }

  /**
   * Backoff.
   */
  public synchronized long getEarliestNextSync() {
    return accountSharedPreferences.getLong(SyncConfiguration.PREF_EARLIEST_NEXT_SYNC, 0);
  }

  public synchronized void setEarliestNextSync(long next) {
    Editor edit = accountSharedPreferences.edit();
    edit.putLong(SyncConfiguration.PREF_EARLIEST_NEXT_SYNC, next);
    edit.commit();
  }

  public synchronized void extendEarliestNextSync(long next) {
    if (accountSharedPreferences.getLong(SyncConfiguration.PREF_EARLIEST_NEXT_SYNC, 0) >= next) {
      return;
    }
    Editor edit = accountSharedPreferences.edit();
    edit.putLong(SyncConfiguration.PREF_EARLIEST_NEXT_SYNC, next);
    edit.commit();
  }

  private void handleException(Exception e, SyncResult syncResult) {
    try {
      if (e instanceof SQLiteConstraintException) {
        Log.e(LOG_TAG, "Constraint exception. Aborting sync.", e);
        syncResult.stats.numParseExceptions++;       // This is as good as we can do.
        return;
      }
      if (e instanceof SQLiteException) {
        Log.e(LOG_TAG, "Couldn't open database (locked?). Aborting sync.", e);
        syncResult.stats.numIoExceptions++;
        return;
      }
      if (e instanceof OperationCanceledException) {
        Log.e(LOG_TAG, "Operation canceled. Aborting sync.", e);
        return;
      }
      if (e instanceof AuthenticatorException) {
        syncResult.stats.numParseExceptions++;
        Log.e(LOG_TAG, "AuthenticatorException. Aborting sync.", e);
        return;
      }
      if (e instanceof IOException) {
        syncResult.stats.numIoExceptions++;
        Log.e(LOG_TAG, "IOException. Aborting sync.", e);
        e.printStackTrace();
        return;
      }
      syncResult.stats.numIoExceptions++;
      Log.e(LOG_TAG, "Unknown exception. Aborting sync.", e);
    } finally {
      notifyMonitor();
    }
  }

  @Override
  public void onSyncCanceled() {
    super.onSyncCanceled();
    // TODO: cancel the sync!
    // From the docs: "This will be invoked on a separate thread than the sync
    // thread and so you must consider the multi-threaded implications of the
    // work that you do in this method."
  }

  public Object syncMonitor = new Object();
  private SyncResult syncResult;

  public Account localAccount;
  protected boolean thisSyncIsForced = false;
  public SharedPreferences accountSharedPreferences;

  /**
   * Return the number of milliseconds until we're allowed to sync again,
   * or 0 if now is fine.
   */
  public long delayMilliseconds() {
    long earliestNextSync = getEarliestNextSync();
    if (earliestNextSync <= 0) {
      return 0;
    }
    long now = System.currentTimeMillis();
    return Math.max(0, earliestNextSync - now);
  }

  @Override
  public boolean shouldBackOff() {
    if (thisSyncIsForced) {
      /*
       * If the user asks us to sync, we should sync regardless. This path is
       * hit if the user force syncs and we restart a session after a
       * freshStart.
       */
      return false;
    }

    if (wantNodeAssignment()) {
      /*
       * We recently had a 401 and we aborted the last sync. We should kick off
       * another sync to fetch a new node/weave cluster URL, since ours is
       * stale. If we have a user authentication error, the next sync will
       * determine that and will stop requesting node assignment, so this will
       * only force one abnormally scheduled sync.
       */
      return false;
    }

    return delayMilliseconds() > 0;
  }

  /**
   * Asynchronously request an immediate sync, optionally syncing only the given
   * named stages.
   * <p>
   * Returns immediately.
   *
   * @param account
   *          the Android <code>Account</code> instance to sync.
   * @param stageNames
   *          stage names to sync, or <code>null</code> to sync all known stages.
   */
  public static void requestImmediateSync(final Account account, final String[] stageNames) {
    if (account == null) {
      Logger.warn(LOG_TAG, "Not requesting immediate sync because Android Account is null.");
      return;
    }

    final Bundle extras = new Bundle();
    Utils.putStageNamesToSync(extras, stageNames, null);
    extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
    ContentResolver.requestSync(account, BrowserContract.AUTHORITY, extras);
  }

  @Override
  public void onPerformSync(final Account account,
                            final Bundle extras,
                            final String authority,
                            final ContentProviderClient provider,
                            final SyncResult syncResult) {
    Log.d(LOG_TAG, "Got onPerformSync. Extras bundle is " + extras + ".");

    Logger.resetLogging();
    Utils.reseedSharedRandom(); // Make sure we don't work with the same random seed for too long.

    // Set these so that we don't need to thread them through assorted calls and callbacks.
    this.syncResult   = syncResult;
    this.localAccount = account;

    final SyncAccountParameters params = SyncAccounts.blockingFromAndroidAccountV0(mContext, AccountManager.get(mContext), this.localAccount);

    final AtomicBoolean setNextSync = new AtomicBoolean(true);
    final SyncAdapter self = this;
    final Runnable runnable = new Runnable() {
      @Override
      public void run() {
        Logger.trace(LOG_TAG, "AccountManagerCallback invoked.");
        // TODO: N.B.: Future must not be used on the main thread.
        try {
          if (params == null) {
            Log.e(LOG_TAG, "No account parameters: aborting sync.");
            syncResult.stats.numAuthExceptions++;
            notifyMonitor();
            return;
          }

          String username  = params.username; // Encoded with Utils.usernameFromAccount.
          String password  = params.password;
          String serverURL = params.serverURL;
          String syncKey   = params.syncKey;

          Logger.debug(LOG_TAG, "Username: " + username);
          Logger.debug(LOG_TAG, "Server:   " + serverURL);
          if (Logger.LOG_PERSONAL_INFORMATION) {
            Logger.debug(LOG_TAG, "Password: " + password);
            Logger.debug(LOG_TAG, "Sync key: " + syncKey);
          } else {
            Logger.debug(LOG_TAG, "Password? " + (password != null));
            Logger.debug(LOG_TAG, "Sync key? " + (syncKey != null));
          }

          if (password  == null &&
              username  == null &&
              syncKey   == null &&
              serverURL == null) {

            // Totally blank. Most likely the user has two copies of Firefox
            // installed, and something is misbehaving.
            // Disable this account.
            Logger.error(LOG_TAG, "No credentials attached to account. Aborting sync.");
            try {
              SyncAccounts.setSyncAutomatically(account, false);
            } catch (Exception e) {
              Logger.error(LOG_TAG, "Unable to disable account " + account.name + " for " + authority + ".", e);
            }
            syncResult.stats.numAuthExceptions++;
            localAccount = null;
            notifyMonitor();
            return;
          }

          // Now catch the individual cases.
          if (password == null) {
            Log.e(LOG_TAG, "No password: aborting sync.");
            syncResult.stats.numAuthExceptions++;
            notifyMonitor();
            return;
          }

          if (syncKey == null) {
            Log.e(LOG_TAG, "No sync key: aborting sync.");
            syncResult.stats.numAuthExceptions++;
            notifyMonitor();
            return;
          }

          // Support multiple accounts by mapping each server/account pair to a branch of the
          // shared preferences space.
          final String product = GlobalConstants.BROWSER_INTENT_PACKAGE;
          final String profile = Constants.DEFAULT_PROFILE;
          final long version = SyncConfiguration.CURRENT_PREFS_VERSION;
          self.accountSharedPreferences = Utils.getSharedPreferences(mContext, product, username, serverURL, profile, version);

          Log.i(LOG_TAG,
              "Syncing account named " + account.name +
              " for client named '" + getClientName() +
              "' with client guid " + getAccountGUID() +
              " (sync account has " + getClientsCount() + " clients).");

          thisSyncIsForced = (extras != null) && (extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false));
          long delay = delayMilliseconds();
          if (delay > 0) {
            if (thisSyncIsForced) {
              Log.i(LOG_TAG, "Forced sync: overruling remaining backoff of " + delay + "ms.");
            } else {
              Log.i(LOG_TAG, "Not syncing: must wait another " + delay + "ms.");
              long remainingSeconds = delay / 1000;
              syncResult.delayUntil = remainingSeconds + BACKOFF_PAD_SECONDS;
              setNextSync.set(false);
              self.notifyMonitor();
              return;
            }
          }

          final KeyBundle keyBundle = new KeyBundle(username, syncKey);
          final String prefsPath = Utils.getPrefsPath(product, username, serverURL, profile, version);
          self.performSync(account, extras, authority, provider, syncResult,
              username, password, prefsPath, serverURL, keyBundle);
        } catch (Exception e) {
          self.handleException(e, syncResult);
          return;
        }
      }
    };

    synchronized (syncMonitor) {
      // Perform the work in a new thread from within this synchronized block,
      // which allows us to be waiting on the monitor before the callback can
      // notify us in a failure case. Oh, concurrent programming.
      new Thread(runnable).start();

      // Start our stale connection monitor thread.
      ConnectionMonitorThread stale = new ConnectionMonitorThread();
      stale.start();

      Logger.trace(LOG_TAG, "Waiting on sync monitor.");
      try {
        syncMonitor.wait();
        if (setNextSync.get()) {
          long interval = getSyncInterval();
          long next = System.currentTimeMillis() + interval;
          Log.i(LOG_TAG, "Setting minimum next sync time to " + next + " (" + interval + "ms from now).");
          extendEarliestNextSync(next);
        }
      } catch (InterruptedException e) {
        Log.w(LOG_TAG, "Waiting on sync monitor interrupted.", e);
      } finally {
        // And we're done with HTTP stuff.
        stale.shutdown();
      }
    }
  }

  public int getSyncInterval() {
    // Must have been a problem that means we can't access the Account.
    if (this.localAccount == null) {
      return SINGLE_DEVICE_INTERVAL_MILLISECONDS;
    }

    int clientsCount = this.getClientsCount();
    if (clientsCount <= 1) {
      return SINGLE_DEVICE_INTERVAL_MILLISECONDS;
    }

    return MULTI_DEVICE_INTERVAL_MILLISECONDS;
  }


  /**
   * Now that we have a sync key and password, go ahead and do the work.
   * @param prefsPath TODO
   * @throws NoSuchAlgorithmException
   * @throws IllegalArgumentException
   * @throws SyncConfigurationException
   * @throws AlreadySyncingException
   * @throws NonObjectJSONException
   * @throws ParseException
   * @throws IOException
   */
  protected void performSync(Account account, Bundle extras, String authority,
                             ContentProviderClient provider,
                             SyncResult syncResult,
                             String username, String password,
                             String prefsPath,
                             String serverURL, KeyBundle keyBundle)
                                 throws NoSuchAlgorithmException,
                                        SyncConfigurationException,
                                        IllegalArgumentException,
                                        AlreadySyncingException,
                                        IOException, ParseException,
                                        NonObjectJSONException {
    Logger.trace(LOG_TAG, "Performing sync.");

    // TODO: default serverURL.
    GlobalSession globalSession = new GlobalSession(SyncConfiguration.DEFAULT_USER_API,
                                                    serverURL, username, password, prefsPath,
                                                    keyBundle, this, this.mContext, extras, this);

    globalSession.start();
  }

  private void notifyMonitor() {
    synchronized (syncMonitor) {
      Logger.trace(LOG_TAG, "Notifying sync monitor.");
      syncMonitor.notifyAll();
    }
  }

  // Implementing GlobalSession callbacks.
  @Override
  public void handleError(GlobalSession globalSession, Exception ex) {
    Log.i(LOG_TAG, "GlobalSession indicated error. Flagging auth token as invalid, just in case.");
    this.updateStats(globalSession, ex);
    notifyMonitor();
  }

  @Override
  public void handleAborted(GlobalSession globalSession, String reason) {
    Log.w(LOG_TAG, "Sync aborted: " + reason);
    notifyMonitor();
  }

  /**
   * Introspect the exception, incrementing the appropriate stat counters.
   * TODO: increment number of inserts, deletes, conflicts.
   *
   * @param globalSession
   * @param ex
   */
  private void updateStats(GlobalSession globalSession,
                           Exception ex) {
    if (ex instanceof SyncException) {
      ((SyncException) ex).updateStats(globalSession, syncResult);
    }
    // TODO: non-SyncExceptions.
    // TODO: wouldn't it be nice to update stats for *every* exception we get?
  }

  @Override
  public void handleSuccess(GlobalSession globalSession) {
    Log.i(LOG_TAG, "GlobalSession indicated success.");
    Logger.debug(LOG_TAG, "Prefs target: " + globalSession.config.prefsPath);
    globalSession.config.persistToPrefs();
    notifyMonitor();
  }

  @Override
  public void handleStageCompleted(Stage currentState,
                                   GlobalSession globalSession) {
    Logger.trace(LOG_TAG, "Stage completed: " + currentState);
  }

  @Override
  public void requestBackoff(long backoff) {
    if (backoff > 0) {
      this.extendEarliestNextSync(System.currentTimeMillis() + backoff);
    }
  }

  @Override
  public synchronized String getAccountGUID() {
    String accountGUID = accountSharedPreferences.getString(SyncConfiguration.PREF_ACCOUNT_GUID, null);
    if (accountGUID == null) {
      Logger.debug(LOG_TAG, "Account GUID was null. Creating a new one.");
      accountGUID = Utils.generateGuid();
      accountSharedPreferences.edit().putString(SyncConfiguration.PREF_ACCOUNT_GUID, accountGUID).commit();
    }
    return accountGUID;
  }

  @Override
  public synchronized String getClientName() {
    String clientName = accountSharedPreferences.getString(SyncConfiguration.PREF_CLIENT_NAME, null);
    if (clientName == null) {
      clientName = GlobalConstants.PRODUCT_NAME + " on " + android.os.Build.MODEL;
      accountSharedPreferences.edit().putString(SyncConfiguration.PREF_CLIENT_NAME, clientName).commit();
    }
    return clientName;
  }

  @Override
  public synchronized void setClientsCount(int clientsCount) {
    accountSharedPreferences.edit().putLong(SyncConfiguration.PREF_NUM_CLIENTS, (long) clientsCount).commit();
  }

  @Override
  public boolean isLocalGUID(String guid) {
    return getAccountGUID().equals(guid);
  }

  @Override
  public synchronized int getClientsCount() {
    return (int) accountSharedPreferences.getLong(SyncConfiguration.PREF_NUM_CLIENTS, 0);
  }

  public synchronized boolean getClusterURLIsStale() {
    return accountSharedPreferences.getBoolean(SyncConfiguration.PREF_CLUSTER_URL_IS_STALE, false);
  }

  public synchronized void setClusterURLIsStale(boolean clusterURLIsStale) {
    Editor edit = accountSharedPreferences.edit();
    edit.putBoolean(SyncConfiguration.PREF_CLUSTER_URL_IS_STALE, clusterURLIsStale);
    edit.commit();
  }

  @Override
  public boolean wantNodeAssignment() {
    return getClusterURLIsStale();
  }

  @Override
  public void informNodeAuthenticationFailed(GlobalSession session, URI failedClusterURL) {
    // TODO: communicate to the user interface that we need a new user password!
    // TODO: only freshen the cluster URL (better yet, forget the cluster URL) after the user has provided new credentials.
    setClusterURLIsStale(false);
  }

  @Override
  public void informNodeAssigned(GlobalSession session, URI oldClusterURL, URI newClusterURL) {
    setClusterURLIsStale(false);
  }

  @Override
  public void informUnauthorizedResponse(GlobalSession session, URI oldClusterURL) {
    setClusterURLIsStale(true);
  }

  @Override
  public void informUpgradeRequiredResponse(final GlobalSession session) {
    final AccountManager manager = AccountManager.get(mContext);
    final Account toDisable      = localAccount;
    if (toDisable == null || manager == null) {
      Logger.warn(LOG_TAG, "Attempting to disable account, but null found.");
      return;
    }
    // Sync needs to be upgraded. Don't automatically sync anymore.
    ThreadPool.run(new Runnable() {
      @Override
      public void run() {
        manager.setUserData(toDisable, Constants.DATA_ENABLE_ON_UPGRADE, "1");
        SyncAccounts.setSyncAutomatically(toDisable, false);
      }
    });
  }
}
