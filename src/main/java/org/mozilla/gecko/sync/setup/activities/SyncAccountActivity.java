/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.setup.activities;

import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.SyncConstants;
import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.setup.SyncAccounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Shared superclass of Sync activities that need a Sync account to exist. If an
 * account does not exist, redirects the user to set up Sync.
 * <p>
 * Subclass <code>SyncAccountActivity</code> and call super's
 * {@link #onResume()}. This synchronously queries the Android account list, and
 * sets {@link #localAccount} if a Sync account exists or redirects if a Sync
 * account does not exist. Remember to check if <code>localAccount</code> is
 * null -- <code>onResume</code> calls {@link #finish()}, but your child's
 * <code>onResume</code> method will continue.
 */
public abstract class SyncAccountActivity extends SyncActivity {
  public static final String LOG_TAG = SyncAccountActivity.class.getSimpleName();

  protected AccountManager accountManager;
  protected Account localAccount;

  @Override
  public void onResume() {
    Logger.debug(LOG_TAG, "onResume");
    super.onResume();

    redirectIfNoSyncAccount();
  }

  protected void redirectIfNoSyncAccount() {
    Logger.info(LOG_TAG, "redirectIfNoSyncAccount");

    accountManager = AccountManager.get(getApplicationContext());
    Account[] accts = accountManager.getAccountsByType(SyncConstants.ACCOUNTTYPE_SYNC);

    // A Sync account exists.
    if (accts.length > 0) {
      localAccount = accts[0];
      return;
    }

    Intent intent = new Intent(this, RedirectToSetupActivity.class);
    intent.setFlags(Constants.FLAG_ACTIVITY_REORDER_TO_FRONT_NO_ANIMATION);
    startActivity(intent);
    finish();
  }

  /**
   * @return Return null if there is no account set up. Return the account GUID otherwise.
   */
  protected String getAccountGUID() {
    if (localAccount == null) {
      Logger.warn(LOG_TAG, "Null local account; aborting.");
      return null;
    }

    SharedPreferences prefs;
    try {
      prefs = SyncAccounts.blockingPrefsFromDefaultProfileV0(this, accountManager, localAccount);
      return prefs.getString(SyncConfiguration.PREF_ACCOUNT_GUID, null);
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Could not get Sync account parameters or preferences; aborting.");
      return null;
    }
  }
}
