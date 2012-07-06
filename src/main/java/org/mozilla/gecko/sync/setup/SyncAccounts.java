/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.setup;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.sync.GlobalConstants;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.android.RepoUtils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

/**
 * This class contains utilities that are of use to Fennec
 * and Sync setup activities.
 * <p>
 * Do not break these APIs without correcting upstream code!
 */
public class SyncAccounts {

  public final static String DEFAULT_SERVER = "https://auth.services.mozilla.com/";
  private static final String LOG_TAG = "SyncAccounts";

  /**
   * Returns true if a Sync account is set up.
   * <p>
   * Do not call this method from the main thread.
   */
  public static boolean syncAccountsExist(Context c) {
    return AccountManager.get(c).getAccountsByType(Constants.ACCOUNTTYPE_SYNC).length > 0;
  }

  /**
   * This class provides background-thread abstracted access to whether a
   * Firefox Sync account has been set up on this device.
   * <p>
   * Subclass this task and override `onPostExecute` to act on the result.
   */
  public static class AccountsExistTask extends AsyncTask<Context, Void, Boolean> {
    @Override
    protected Boolean doInBackground(Context... params) {
      Context c = params[0];
      return syncAccountsExist(c);
    }
  }

  /**
   * This class encapsulates the parameters needed to create a new Firefox Sync
   * account.
   */
  public static class SyncAccountParameters {
    public final Context context;
    public final AccountManager accountManager;


    public final String username;   // services.sync.account
    public final String syncKey;    // in password manager: "chrome://weave (Mozilla Services Encryption Passphrase)"
    public final String password;   // in password manager: "chrome://weave (Mozilla Services Password)"
    public final String serverURL;  // services.sync.serverURL
    public final String clusterURL; // services.sync.clusterURL
    public final String clientName; // services.sync.client.name
    public final String clientGuid; // services.sync.client.GUID

    /**
     * Encapsulate the parameters needed to create a new Firefox Sync account.
     *
     * @param context
     *          the current <code>Context</code>; cannot be null.
     * @param accountManager
     *          an <code>AccountManager</code> instance to use; if null, get it
     *          from <code>context</code>.
     * @param username
     *          the desired username; cannot be null.
     * @param syncKey
     *          the desired sync key; cannot be null.
     * @param password
     *          the desired password; cannot be null.
     * @param serverURL
     *          the server URL to use; if null, use the default.
     * @param clusterURL
     *          the cluster URL to use; if null, a fresh cluster URL will be
     *          retrieved from the server during the next sync.
     * @param clientName
     *          the client name; if null, a fresh client record will be uploaded
     *          to the server during the next sync.
     * @param clientGuid
     *          the client GUID; if null, a fresh client record will be uploaded
     *          to the server during the next sync.
     */
    public SyncAccountParameters(Context context, AccountManager accountManager,
        String username, String syncKey, String password,
        String serverURL, String clusterURL,
        String clientName, String clientGuid) {
      if (context == null) {
        throw new IllegalArgumentException("Null context passed to SyncAccountParameters constructor.");
      }
      if (username == null) {
        throw new IllegalArgumentException("Null username passed to SyncAccountParameters constructor.");
      }
      if (syncKey == null) {
        throw new IllegalArgumentException("Null syncKey passed to SyncAccountParameters constructor.");
      }
      if (password == null) {
        throw new IllegalArgumentException("Null password passed to SyncAccountParameters constructor.");
      }
      this.context = context;
      this.accountManager = accountManager;
      this.username = username;
      this.syncKey = syncKey;
      this.password = password;
      this.serverURL = serverURL;
      this.clusterURL = clusterURL;
      this.clientName = clientName;
      this.clientGuid = clientGuid;
    }

    public SyncAccountParameters(Context context, AccountManager accountManager,
        String username, String syncKey, String password, String serverURL) {
      this(context, accountManager, username, syncKey, password, serverURL, null, null, null);
    }
  }

  /**
   * This class provides background-thread abstracted access to creating a
   * Firefox Sync account.
   * <p>
   * Subclass this task and override `onPostExecute` to act on the result. The
   * <code>Result</code> (of type <code>Account</code>) is null if an error
   * occurred and the account could not be added.
   */
  public static class CreateSyncAccountTask extends AsyncTask<SyncAccountParameters, Void, Account> {
    protected final boolean syncAutomatically;

    public CreateSyncAccountTask() {
      this.syncAutomatically = true;
    }

    public CreateSyncAccountTask(final boolean syncAutomically) {
      this.syncAutomatically = syncAutomically;
    }

    @Override
    protected Account doInBackground(SyncAccountParameters... params) {
      SyncAccountParameters syncAccount = params[0];
      try {
        return createSyncAccount(syncAccount, syncAutomatically);
      } catch (Exception e) {
        Log.e(Logger.GLOBAL_LOG_TAG, "Unable to create account.", e);
        return null;
      }
    }
  }

  /**
   * Create a sync account and set it to sync automatically.
   * <p>
   * Do not call this method from the main thread.
   *
   * @param syncAccount
   *          parameters of the account to be created.
   * @return created <code>Account</code>, or null if an error occurred and the
   *         account could not be added.
   */
  public static Account createSyncAccount(SyncAccountParameters syncAccount) {
    return createSyncAccount(syncAccount, true);
  }

  /**
   * Create a sync account.
   * <p>
   * Do not call this method from the main thread.
   * <p>
   * Intended for testing; use
   * <code>createSyncAccount(SyncAccountParameters)</code> instead.
   *
   * @param syncAccount
   *          parameters of the account to be created.
   * @param syncAutomatically
   *          whether to start syncing this Account automatically (
   *          <code>false</code> for test accounts).
   * @return created Android <code>Account</code>, or null if an error occurred
   *         and the account could not be added.
   */
  public static Account createSyncAccount(SyncAccountParameters syncAccount, boolean syncAutomatically) {
    final Context context = syncAccount.context;
    final AccountManager accountManager = (syncAccount.accountManager == null) ?
          AccountManager.get(syncAccount.context) : syncAccount.accountManager;
    final String username  = syncAccount.username;
    final String syncKey   = syncAccount.syncKey;
    final String password  = syncAccount.password;
    final String serverURL = (syncAccount.serverURL == null) ?
        DEFAULT_SERVER : syncAccount.serverURL;

    Logger.debug(LOG_TAG, "Using account manager " + accountManager);
    if (!RepoUtils.stringsEqual(syncAccount.serverURL, DEFAULT_SERVER)) {
      Logger.info(LOG_TAG, "Setting explicit server URL: " + serverURL);
    }

    final Account account = new Account(username, Constants.ACCOUNTTYPE_SYNC);
    final Bundle userbundle = new Bundle();

    // Add sync key and server URL.
    userbundle.putString(Constants.OPTION_SYNCKEY, syncKey);
    userbundle.putString(Constants.OPTION_SERVER, serverURL);
    Logger.debug(LOG_TAG, "Adding account for " + Constants.ACCOUNTTYPE_SYNC);
    boolean result = false;
    try {
      result = accountManager.addAccountExplicitly(account, password, userbundle);
    } catch (SecurityException e) {
      // We use Log rather than Logger here to avoid possibly hiding these errors.
      final String message = e.getMessage();
      if (message != null && (message.indexOf("is different than the authenticator's uid") > 0)) {
        Log.wtf(Logger.GLOBAL_LOG_TAG,
                "Unable to create account. " +
                "If you have more than one version of " +
                "Firefox/Beta/Aurora/Nightly/Fennec installed, that's why.",
                e);
      } else {
        Log.e(Logger.GLOBAL_LOG_TAG, "Unable to create account.", e);
      }
    }

    if (!result) {
      Logger.error(LOG_TAG, "Failed to add account " + account + "!");
      return null;
    }
    Logger.debug(LOG_TAG, "Account " + account + " added successfully.");

    setSyncAutomatically(account, syncAutomatically);
    setIsSyncable(account, syncAutomatically);
    Logger.debug(LOG_TAG, "Set account to sync automatically? " + syncAutomatically + ".");

    try {
      final String product = GlobalConstants.BROWSER_INTENT_PACKAGE;
      final String profile = Constants.DEFAULT_PROFILE;
      final long version = SyncConfiguration.CURRENT_PREFS_VERSION;
      Logger.info(LOG_TAG, "Clearing preferences path " + Utils.getPrefsPath(product, username, serverURL, profile, version) + " for this account.");
      SharedPreferences.Editor editor = Utils.getSharedPreferences(context, product, username, serverURL, profile, version).edit().clear();

      if (syncAccount.clusterURL != null) {
        editor.putString(SyncConfiguration.PREF_CLUSTER_URL, syncAccount.clusterURL);
      }

      if (syncAccount.clientName != null && syncAccount.clientGuid != null) {
        Logger.debug(LOG_TAG, "Setting client name to " + syncAccount.clientName + " and client GUID to " + syncAccount.clientGuid + ".");
        editor.putString(SyncConfiguration.PREF_CLIENT_NAME, syncAccount.clientName);
        editor.putString(SyncConfiguration.PREF_ACCOUNT_GUID, syncAccount.clientGuid);
      } else {
        Logger.debug(LOG_TAG, "Client name and guid not both non-null, so not setting client data.");
      }

      editor.commit();
    } catch (Exception e) {
      Logger.error(LOG_TAG, "Could not clear prefs path!", e);
    }
    return account;
  }

  public static void setIsSyncable(Account account, boolean isSyncable) {
    String authority = BrowserContract.AUTHORITY;
    ContentResolver.setIsSyncable(account, authority, isSyncable ? 1 : 0);
  }

  public static void setSyncAutomatically(Account account, boolean syncAutomatically) {
    if (syncAutomatically) {
      ContentResolver.setMasterSyncAutomatically(true);
    }

    String authority = BrowserContract.AUTHORITY;
    Logger.debug(LOG_TAG, "Setting authority " + authority + " to " +
                          (syncAutomatically ? "" : "not ") + "sync automatically.");
    ContentResolver.setSyncAutomatically(account, authority, syncAutomatically);
  }

  protected static class SyncAccountVersion0Callback implements AccountManagerCallback<Bundle> {
    protected final Context context;
    protected final CountDownLatch latch;

    public SyncAccountParameters syncAccountParameters = null;

    public SyncAccountVersion0Callback(final Context context, final CountDownLatch latch) {
      this.context = context;
      this.latch = latch;
    }

    @Override
    public void run(AccountManagerFuture<Bundle> future) {
      try {
        Bundle bundle = future.getResult(60L, TimeUnit.SECONDS);
        if (bundle.containsKey("KEY_INTENT")) {
          throw new IllegalStateException("KEY_INTENT included in AccountManagerFuture bundle.");
        }
        final String username  = bundle.getString(Constants.OPTION_USERNAME); // Encoded by Utils.usernameFromAccount.
        final String syncKey   = bundle.getString(Constants.OPTION_SYNCKEY);
        final String serverURL = bundle.getString(Constants.OPTION_SERVER);
        final String password  = bundle.getString(AccountManager.KEY_AUTHTOKEN);

        syncAccountParameters = new SyncAccountParameters(this.context, null, username, syncKey, password, serverURL);
      } catch (Exception e) {
        // Do nothing -- caller will find null syncAccountParameters.
        Logger.warn(LOG_TAG, "Got exception fetching Sync account parameters.", e);
      } finally {
        latch.countDown();
      }
    }
  }

  /**
   * Synchronously extract Sync account parameters from Android account version
   * 0, using plain auth token type.
   * <p>
   * Safe to call from main thread.
   * <p>
   * Invalidates existing auth token first, which is necessary because Android
   * caches only the auth token string, not the complete bundle. By invalidating
   * the existing token, we generate a new (complete) bundle every invocation.
   *
   * @param context
   * @param accountManager
   *          Android account manager.
   * @param account
   *          Android account.
   * @return Sync account parameters.
   */
  public static SyncAccountParameters blockingFromAndroidAccountV0(final Context context, final AccountManager accountManager, final Account account) {
    final CountDownLatch latch = new CountDownLatch(1);
    final SyncAccountVersion0Callback callback = new SyncAccountVersion0Callback(context, latch);

    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          // Get old auth token, with incomplete bundle.
          String oldToken = accountManager.getAuthToken(account, Constants.AUTHTOKEN_TYPE_PLAIN, true, null, null).getResult().getString(AccountManager.KEY_AUTHTOKEN);
          // Invalidate it.
          accountManager.invalidateAuthToken(Constants.ACCOUNTTYPE_SYNC, oldToken); // Not the token type, the account type!
        } catch (Exception e) {
          Logger.warn(LOG_TAG, "Got exception invalidating old token.", e);
          latch.notify();
        } finally {
        }

        // Get the new auth token, with complete bundle.
        accountManager.getAuthToken(account, Constants.AUTHTOKEN_TYPE_PLAIN, true, callback, null);
      }
    }).start();

    try {
      latch.await();
    } catch (InterruptedException e) {
      Logger.warn(LOG_TAG, "Got exception waiting for Sync account parameters.", e);
      return null;
    }

    return callback.syncAccountParameters;
  }

  protected static class SyncAccountJSONRunnable implements Runnable {
    protected final Context context;
    protected final AccountManager accountManager;
    protected final Account account;
    protected final CountDownLatch latch;
    protected final int version;

    public SyncAccountParameters syncAccountParameters = null;

    public SyncAccountJSONRunnable(final Context context, final AccountManager accountManager, final Account account, final CountDownLatch latch, final int version) {
      this.context = context;
      this.accountManager = accountManager;
      this.account = account;
      this.latch = latch;
      this.version = version;
    }

    @Override
    public void run() {
      try {
        // Can try to fetch a specified version, but only accepts version 1 in return (see check below).
        final String token = accountManager.blockingGetAuthToken(account, Constants.AUTHTOKEN_TYPE_JSON_PREFIX + this.version, true);
        if (token == null) {
          throw new IllegalStateException("Could not fetch JSON auth token; old version installed?");
        }

        ExtendedJSONObject o = ExtendedJSONObject.parseJSONObject(token);
        int version = o.getIntegerSafely(Constants.JSON_KEY_VERSION);
        if (version != 1) {
          throw new IllegalStateException("Got auth token JSON version = " + this.version + " != 1.");
        }

        final String username  = o.getString(Constants.JSON_KEY_ACCOUNT); // Encoded by Utils.usernameFromAccount.
        final String syncKey   = o.getString(Constants.JSON_KEY_SYNCKEY);
        final String serverURL = o.getString(Constants.JSON_KEY_SERVER);
        final String password  = o.getString(Constants.JSON_KEY_PASSWORD);

        this.syncAccountParameters = new SyncAccountParameters(context, null, username, syncKey, password, serverURL);
      } catch (Exception e) {
        Logger.warn(LOG_TAG, "Got exception getting auth token.", e);
      } finally {
        latch.countDown();
      }
    }
  }

  /**
   * Synchronously extract Sync account parameters from Android account version
   * >= 1, using JSON auth token type.
   * <p>
   * Safe to call from main thread.
   *
   * @param context
   * @param accountManager
   *          Android account manager.
   * @param account
   *          Android account.
   * @param version
   *          JSON version to request.
   * @return Sync account parameters.
   */
  public static SyncAccountParameters blockingFromAndroidAccount(final Context context, final AccountManager accountManager, final Account account, final int version) {
    final CountDownLatch latch = new CountDownLatch(1);
    final SyncAccountJSONRunnable runnable = new SyncAccountJSONRunnable(context, accountManager, account, latch, version);

    new Thread(runnable).start();

    try {
      latch.await();
    } catch (InterruptedException e) {
      Logger.warn(LOG_TAG, "Got exception waiting for Sync account parameters.", e);
      return null;
    }

    return runnable.syncAccountParameters;
  }
}
