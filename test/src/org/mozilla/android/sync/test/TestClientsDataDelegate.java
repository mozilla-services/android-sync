/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import java.util.concurrent.TimeUnit;

import org.mozilla.gecko.sync.GlobalConstants;
import org.mozilla.gecko.sync.delegates.ClientsDataDelegate;
import org.mozilla.gecko.sync.syncadapter.SyncAdapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.os.Bundle;

public class TestClientsDataDelegate extends AndroidSyncTestCase {
  public static final String TEST_ACCOUNTTYPE = GlobalConstants.ACCOUNTTYPE_SYNC;

  private Context context;
  private ClientsDataDelegate clientsDelegate;
  private AccountManager accountManager;
  private Account account = null;
  private final Bundle userbundle = new Bundle();
  public int numAccountsBefore;

  public void setUp() {
    context = getApplicationContext();
    accountManager = AccountManager.get(context);
    clientsDelegate = new SyncAdapter(context, false);

    // If Android accounts get added while a test runs, this could race, but
    // that's unlikely -- and this is test code.
    numAccountsBefore = accountManager.getAccountsByType(TEST_ACCOUNTTYPE).length;
    account = new Account("testAccount@mozilla.com", TEST_ACCOUNTTYPE);
    ((SyncAdapter)clientsDelegate).localAccount = account;
    assertTrue(accountManager.addAccountExplicitly(account, "", userbundle));
  }

  public void tearDown() {
    // Cleanup.
    clientsDelegate.setClientsCount(0);
    if (account != null) {
      deleteAccount(account);
      account = null;
    }
    int numAccountsAfter = accountManager.getAccountsByType(TEST_ACCOUNTTYPE).length;
    assertEquals(numAccountsBefore, numAccountsAfter);
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

  public void testAccountGUID() {
    // Test getAccountGUID() saved the value it returned the first time.
    String accountGUID = clientsDelegate.getAccountGUID();
    assertNotNull(accountGUID);
    assertEquals(accountGUID, clientsDelegate.getAccountGUID());

    // Test setting the GUID.
    final String TEST_GUID = "testGuid";
    SyncAdapter.setAccountGUID(accountManager, account, TEST_GUID);
    assertEquals(TEST_GUID, clientsDelegate.getAccountGUID());
  }

  public void testClientName() {
    // Test getClientName() saved the value it returned the first time.
    String clientName = clientsDelegate.getClientName();
    assertNotNull(clientName);
    assertEquals(clientName, clientsDelegate.getClientName());

    // Test setting the name.
    final String TEST_NAME = "testName";
    SyncAdapter.setClientName(accountManager, account, TEST_NAME);
    assertEquals(TEST_NAME, clientsDelegate.getClientName());
  }

  public void testClientsCount() {
    final int CLIENTS_COUNT = 15;

    assertEquals(0, clientsDelegate.getClientsCount());
    clientsDelegate.setClientsCount(CLIENTS_COUNT);
    assertEquals(CLIENTS_COUNT, clientsDelegate.getClientsCount());
  }

  public void testIsLocalGUID() {
    assertTrue(clientsDelegate.isLocalGUID(clientsDelegate.getAccountGUID()));
  }

  public void testGetSyncInterval() {
    assertEquals(0, clientsDelegate.getClientsCount());
    assertEquals(SyncAdapter.SINGLE_DEVICE_INTERVAL_MILLISECONDS, ((SyncAdapter)clientsDelegate).getSyncInterval());

    clientsDelegate.setClientsCount(1);
    assertEquals(SyncAdapter.SINGLE_DEVICE_INTERVAL_MILLISECONDS, ((SyncAdapter)clientsDelegate).getSyncInterval());

    clientsDelegate.setClientsCount(2);
    assertEquals(SyncAdapter.MULTI_DEVICE_INTERVAL_MILLISECONDS, ((SyncAdapter)clientsDelegate).getSyncInterval());
  }
}
