package org.mozilla.gecko.sync.receivers;

import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.ThreadPool;
import org.mozilla.gecko.sync.setup.SyncAccounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UpgradeReceiver extends BroadcastReceiver {
  private static final String LOG_TAG = "UpgradeReceiver";

  @Override
  public void onReceive(final Context context, Intent intent) {
    Logger.debug(LOG_TAG, "Broadcast received.");
    // Should filter for specific MY_PACKAGE_REPLACED intent, but Android does
    // not expose it.
    ThreadPool.run(new Runnable() {
      @Override
      public void run() {
        Account[] accounts = AccountManager.get(context).getAccounts();
        SyncAccounts.enableSyncAccounts(accounts, true);
      }
    });
  }
}
