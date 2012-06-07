package org.mozilla.gecko.sync.config.activities;

import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.log.Logger;
import org.mozilla.gecko.sync.setup.SyncAccounts.SyncAccountParameters;
import org.mozilla.gecko.sync.syncadapter.SyncAdapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class AndroidSyncConfigureActivity extends Activity {
  public final static String LOG_TAG = "AndroidSyncConfigAct";

  public interface WithAccountAndPreferences {
    public void run(SyncAccountParameters params, SharedPreferences prefs);
  }

  protected void withAccountAndPreferences(final WithAccountAndPreferences withAccountAndPreferences) {
    final Context context = this;
    Account account = (Account) this.getIntent().getExtras().get("account");
    if (account == null) {
      Logger.error(LOG_TAG, "Failed to get account!");
      return;
    }

    SyncAdapter.withSyncAccountParameters(this, AccountManager.get(this), account, new SyncAdapter.WithSyncAccountParameters() {
      @Override
      public void run(SyncAccountParameters syncAccountParameters) {
        String prefsPath = null;
        try {
          prefsPath = Utils.getPrefsPath(syncAccountParameters.username, syncAccountParameters.serverURL);
        } catch (Exception e) {
          Logger.error(LOG_TAG, "Caught exception getting preferences path.", e);
          return;
        }
        if (prefsPath == null) {
          Logger.error(LOG_TAG, "Got null preferences path.");
          return;
        }

        SharedPreferences prefs = context.getSharedPreferences(prefsPath, Utils.SHARED_PREFERENCES_MODE);
        withAccountAndPreferences.run(syncAccountParameters, prefs);
      }
    });
  }
}
