/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.sync;

import org.mozilla.gecko.background.helpers.AndroidSyncTestCase;
import org.mozilla.gecko.background.sync.helpers.UpgradeRequiredHelper;
import org.mozilla.gecko.sync.setup.SyncAccounts;
import org.mozilla.gecko.sync.setup.SyncAccounts.SyncAccountParameters;
import org.mozilla.gecko.sync.syncadapter.SyncAdapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

public class MockAccountTest extends AndroidSyncTestCase {
  protected static final String TEST_SERVER = "http://test.ser.ver/";
  protected static final String TEST_USERNAME = "user1";
  protected static final String TEST_PASSWORD = "pass1";
  protected static final String TEST_SYNC_KEY = "abcdeabcdeabcdeabcdeabcdea";
  protected Context context;

  protected static class LeakySyncAdapter extends SyncAdapter {
      public LeakySyncAdapter(Context context, boolean autoInitialize, Account account) {
        super(context, autoInitialize);
        this.localAccount = account;
      }
    }

  @Override
  public void setUp() {
    context = getApplicationContext();
    final AccountManager accountManager = AccountManager.get(context);

    UpgradeRequiredHelper.deleteTestAccount(context, this, TEST_USERNAME);

    // Set up and enable Sync accounts.
    SyncAccountParameters syncAccountParams = new SyncAccountParameters(context, accountManager, TEST_USERNAME, TEST_PASSWORD, TEST_SYNC_KEY, TEST_SERVER, null, null, null);
    final Account account = SyncAccounts.createSyncAccount(syncAccountParams, true);
    assertNotNull(account);
    assertTrue(UpgradeRequiredHelper.syncsAutomatically(account));
    assertTrue(UpgradeRequiredHelper.isSyncable(account));
  }

  public MockAccountTest() {
    super();
  }

  @Override
  public void tearDown() {
    UpgradeRequiredHelper.deleteTestAccount(context, this, TEST_USERNAME);
  }

}