package org.mozilla.gecko.sync.config.activities;


import java.util.concurrent.TimeUnit;

import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.ThreadPool;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.setup.Constants;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

/**
 * Activity for configuring Firefox Sync settings, that does work of fetching account information.
 * @author liuche
 *
 */
public abstract class AndroidSyncConfigureActivity extends Activity {
  public final static String LOG_TAG = "AndroidSyncConfigAct";
  protected AccountManager mAccountManager;
  protected Context mContext;

  protected interface PrefsConsumer {
    public void run(SharedPreferences prefs);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mContext = getApplicationContext();
    mAccountManager = AccountManager.get(mContext);
  }

  // hacky, because auth token storage is broken.
  private void invalidateAuthToken(Account account) {
    AccountManagerFuture<Bundle> future = mAccountManager.getAuthToken(account, Constants.AUTHTOKEN_TYPE_PLAIN, true, null, null);
    String token;
    try {
      token = future.getResult().getString(AccountManager.KEY_AUTHTOKEN);
      mAccountManager.invalidateAuthToken(Constants.ACCOUNTTYPE_SYNC, token);
    } catch (Exception e) {
      Logger.error(LOG_TAG, "Couldn't invalidate auth token: " + e);
    }
  }

  protected Account getAccount() {
    Account[] accounts = mAccountManager.getAccountsByType(Constants.ACCOUNTTYPE_SYNC);
    if (accounts.length == 0) {
      return null;
    } else {
      // Only support one account per type.
      return accounts[0];
    }
  }

  public void fetchPrefsAndConsume(final PrefsConsumer accountConsumer) {
    final Account account = getAccount();
    if (account == null) {
      Logger.error(LOG_TAG, "Failed to get account!");
      finish();
      return;
    }

    // Get authToken and pass to AccountConsumer.
    ThreadPool.run(new Runnable() {

      @Override
      public void run() {
        // Hack to fix broken Account creation.
        invalidateAuthToken(account);
        mAccountManager.getAuthToken(account, Constants.AUTHTOKEN_TYPE_PLAIN, true, new AccountManagerCallback<Bundle>() {

          @Override
          public void run(AccountManagerFuture<Bundle> future) {
            Logger.trace(LOG_TAG, "AccountManagerCallback invoked.");
            // TODO: N.B.: Future must not be used on the main thread.
            try {
              Bundle bundle = future.getResult(60L, TimeUnit.SECONDS);
              // Ignoring KEY_INTENT check.
              String username = bundle.getString(Constants.OPTION_USERNAME);
              String serverURL = bundle.getString(Constants.OPTION_SERVER);
              Logger.debug(LOG_TAG, "username null? " + (null == username) + ", serverURL null? " + (null == serverURL));
              // Get SharedPreferences and display configuration editor.
              String prefsPath;
              prefsPath = Utils.getPrefsPath(username, serverURL);
              SharedPreferences prefs = mContext.getSharedPreferences(prefsPath, Utils.SHARED_PREFERENCES_MODE);
              accountConsumer.run(prefs);
            } catch (Exception e) {
              Logger.error(LOG_TAG, "Failed to get sync information or shared preferences.", e);
              finish();
              return;
            }
          }
        }, null);
      }
    });
  }
}