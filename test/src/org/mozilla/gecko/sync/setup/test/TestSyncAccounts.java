/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.setup.test;

import java.util.concurrent.TimeUnit;

import org.mozilla.android.sync.test.AndroidSyncTestCase;
import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.setup.SyncAccounts;
import org.mozilla.gecko.sync.setup.SyncAccounts.SyncAccountParameters;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Context;

public class TestSyncAccounts extends AndroidSyncTestCase {
  private static final String TEST_USERNAME  = "testAccount@mozilla.com";
  private static final String TEST_SYNCKEY   = "testSyncKey";
  private static final String TEST_PASSWORD  = "testPassword";
  private static final String TEST_SERVERURL = "testServerURL";

  private Account account;
  private Context context;
  private AccountManager accountManager;
  private SyncAccountParameters syncAccount;

  public void setUp() {
    account = null;
    context = getApplicationContext();
    accountManager = AccountManager.get(context);
    syncAccount = new SyncAccountParameters(context, null,
        TEST_USERNAME, TEST_SYNCKEY, TEST_PASSWORD, null);
  }

  public void deleteAccount(final Account account) {
    final AccountManagerCallback<Boolean> callback = new AccountManagerCallback<Boolean>() {
      @Override
      public void run(AccountManagerFuture<Boolean> future) {
        try {
          future.getResult(5L, TimeUnit.SECONDS);
          performNotify();
        } catch (Exception e) {
          performNotify(e);
        }
      }
    };

    performWait(new Runnable() {
      @Override
      public void run() {
        accountManager.removeAccount(account, callback, null);
      }
    });
  }

  public void tearDown() {
    if (account == null) {
      return;
    }
    deleteAccount(account);
  }

  public void testSyncAccountParameters() {
    assertEquals(TEST_USERNAME, syncAccount.username);
    assertNull(syncAccount.accountManager);
    assertNull(syncAccount.serverURL);

    try {
      syncAccount = new SyncAccountParameters(context, null,
          null, TEST_SYNCKEY, TEST_PASSWORD, TEST_SERVERURL);
    } catch (IllegalArgumentException e) {
      return;
    } catch (Exception e) {
      fail("Did not expect exception: " + e);
    }
    fail("Expected IllegalArgumentException.");
  }

  public void testCreateAccount() {
    int before = accountManager.getAccountsByType(Constants.ACCOUNTTYPE_SYNC).length;
    account = SyncAccounts.createSyncAccount(syncAccount);
    int afterCreate = accountManager.getAccountsByType(Constants.ACCOUNTTYPE_SYNC).length;
    assertTrue(afterCreate > before);
    deleteAccount(account);
    account = null;
    int afterDelete = accountManager.getAccountsByType(Constants.ACCOUNTTYPE_SYNC).length;
    assertEquals(before, afterDelete);
  }

  public void testCreateSecondAccount() {
    int before = accountManager.getAccountsByType(Constants.ACCOUNTTYPE_SYNC).length;
    account = SyncAccounts.createSyncAccount(syncAccount);
    int afterFirst = accountManager.getAccountsByType(Constants.ACCOUNTTYPE_SYNC).length;
    assertTrue(afterFirst > before);

    SyncAccountParameters secondSyncAccount = new SyncAccountParameters(context, null,
        "second@username.com", TEST_SYNCKEY, TEST_PASSWORD, null);

    Account second = SyncAccounts.createSyncAccount(secondSyncAccount);
    assertNotNull(second);
    int afterSecond = accountManager.getAccountsByType(Constants.ACCOUNTTYPE_SYNC).length;
    assertTrue(afterSecond > afterFirst);

    deleteAccount(second);
    deleteAccount(account);
    account = null;

    int afterDelete = accountManager.getAccountsByType(Constants.ACCOUNTTYPE_SYNC).length;
    assertEquals(before, afterDelete);
  }

  public void testCreateDuplicateAccount() {
    int before = accountManager.getAccountsByType(Constants.ACCOUNTTYPE_SYNC).length;
    account = SyncAccounts.createSyncAccount(syncAccount);
    int afterCreate = accountManager.getAccountsByType(Constants.ACCOUNTTYPE_SYNC).length;
    assertTrue(afterCreate > before);

    Account dupe = SyncAccounts.createSyncAccount(syncAccount);
    assertNull(dupe);
  }

  public Boolean result;

  public void testAccountsExistTask() {
    int before = accountManager.getAccountsByType(Constants.ACCOUNTTYPE_SYNC).length;
    account = SyncAccounts.createSyncAccount(syncAccount);
    int afterCreate = accountManager.getAccountsByType(Constants.ACCOUNTTYPE_SYNC).length;
    assertTrue(afterCreate > before);

    final TestSyncAccounts self = this;
    final SyncAccounts.AccountsExistTask task = new SyncAccounts.AccountsExistTask() {
      @Override
      public void onPostExecute(Boolean result) {
        self.result = result;
        performNotify();
      }
    };
    performWait(new Runnable() {
      @Override
      public void run() {
        task.execute(context);
      }
    });

    assertNotNull(result);
    assertTrue(result.booleanValue());

    deleteAccount(account);
    account = null;
    int afterDelete = accountManager.getAccountsByType(Constants.ACCOUNTTYPE_SYNC).length;
    assertEquals(before, afterDelete);
  }

  public void testCreateAccountTask() {
    int before = accountManager.getAccountsByType(Constants.ACCOUNTTYPE_SYNC).length;

    final TestSyncAccounts self = this;
    final SyncAccounts.CreateSyncAccountTask task = new SyncAccounts.CreateSyncAccountTask() {
      @Override
      public void onPostExecute(Account account) {
        self.account = account;
        performNotify();
      }
    };
    performWait(new Runnable() {
      @Override
      public void run() {
        task.execute(syncAccount);
      }
    });

    assertNotNull(account);

    int afterCreate = accountManager.getAccountsByType(Constants.ACCOUNTTYPE_SYNC).length;
    assertTrue(afterCreate > before);

    deleteAccount(account);
    account = null;
    int afterDelete = accountManager.getAccountsByType(Constants.ACCOUNTTYPE_SYNC).length;
    assertEquals(before, afterDelete);
  }
}
