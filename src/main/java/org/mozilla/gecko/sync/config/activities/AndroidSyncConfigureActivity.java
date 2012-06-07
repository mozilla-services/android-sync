package org.mozilla.gecko.sync.config.activities;

import org.mozilla.gecko.sync.setup.Constants;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
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

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mContext = getApplicationContext();
    mAccountManager = AccountManager.get(mContext);
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
//  public interface WithAccountAndPreferences {
//    public void run(SyncAccountParameters params, SharedPreferences prefs);
//  }
//
//  protected void withAccountAndPreferences(final WithAccountAndPreferences withAccountAndPreferences) {
//    final Context context = this;
//    Account account = (Account) this.getIntent().getExtras().get("account");
//    if (account == null) {
//      Logger.error(LOG_TAG, "Failed to get account!");
//      return;
//    }
//
//    SyncAdapter.withSyncAccountParameters(this, AccountManager.get(this), account{
//      @Override
//      public void run(SyncAccountParameters syncAccountParameters) {
//        String prefsPath = null;
//        try {
//          prefsPath = Utils.getPrefsPath(syncAccountParameters.username, syncAccountParameters.serverURL);
//        } catch (Exception e) {
//          Logger.error(LOG_TAG, "Caught exception getting preferences path.", e);
//          return;
//        }
//        if (prefsPath == null) {
//          Logger.error(LOG_TAG, "Got null preferences path.");
//          return;
//        }
//
//        SharedPreferences prefs = context.getSharedPreferences(prefsPath, Utils.SHARED_PREFERENCES_MODE);
//        withAccountAndPreferences.run(syncAccountParameters, prefs);
//      }
//    });
//  }
}
