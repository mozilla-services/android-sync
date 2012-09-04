package org.mozilla.gecko.sync.config.activities;


import org.mozilla.gecko.sync.GlobalConstants;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.ThreadPool;
import org.mozilla.gecko.sync.setup.SyncAccounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

/**
 * Activity for configuring Firefox Sync settings, that does work of fetching account information.
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

  /**
   * Fetches the prefs associated with a Sync account, and passes the prefs to
   * the PrefsConsumer.
   *
   * @param accountConsumer interface for consuming prefs from an Account.
   */
  public void fetchPrefsAndConsume(final PrefsConsumer accountConsumer) {
    // Run Account management on separate thread to avoid strict mode policy violations.
    ThreadPool.run(new Runnable() {
      @Override
      public void run() {
        Account[] accounts = mAccountManager.getAccountsByType(GlobalConstants.ACCOUNTTYPE_SYNC);
        if (accounts.length == 0) {
          Logger.error(LOG_TAG, "Failed to get account!");
          finish();
          return;
        } else {
          // Only supports one account per type.
          Account account = accounts[0];
          try {
            SharedPreferences prefs = SyncAccounts.getPrefsFromDefaultAccount(mContext, mAccountManager, account);
            accountConsumer.run(prefs);
          } catch (Exception e) {
            Logger.error(LOG_TAG, "Failed to get sync account info or shared preferences.", e);
            finish();
          }
        }
      }
    });
  }
}