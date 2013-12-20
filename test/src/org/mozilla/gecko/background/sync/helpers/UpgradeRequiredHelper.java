/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.sync.helpers;

import org.mozilla.gecko.background.helpers.AndroidSyncTestCase;
import org.mozilla.gecko.background.sync.TestSyncAccounts;
import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.sync.SyncConstants;
import org.mozilla.gecko.sync.setup.Constants;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;

public class UpgradeRequiredHelper {

  public static boolean syncsAutomatically(Account a) {
    return ContentResolver.getSyncAutomatically(a, BrowserContract.AUTHORITY);
  }

  public static boolean isSyncable(Account a) {
    return 1 == ContentResolver.getIsSyncable(a, BrowserContract.AUTHORITY);
  }

  public static boolean willEnableOnUpgrade(Account a, AccountManager accountManager) {
    return "1".equals(accountManager.getUserData(a, Constants.DATA_ENABLE_ON_UPGRADE));
  }

  public static Account getTestAccount(AccountManager accountManager, String accountName) {
    final String type = SyncConstants.ACCOUNTTYPE_SYNC;
    Account[] existing = accountManager.getAccountsByType(type);
    for (Account account : existing) {
      if (account.name.equals(accountName)) {
        return account;
      }
    }
    return null;
  }

  public static void deleteTestAccount(Context context, AndroidSyncTestCase test, String accountName) {
    final AccountManager accountManager = AccountManager.get(context);
    final Account found = getTestAccount(accountManager, accountName);
    if (found == null) {
      return;
    }
    TestSyncAccounts.deleteAccount(test, accountManager, found);
  }

}
