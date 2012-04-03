/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.gecko.sync.delegates.ClientsDataDelegate;
import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.syncadapter.SyncAdapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Bundle;

public class TestClientsDataDelegate extends AndroidSyncTestCase {
  private Context context;
  private ClientsDataDelegate clientsDelegate;
  private AccountManager accountManager;
  private final Account account = new Account("blah@mozilla.com", Constants.ACCOUNTTYPE_SYNC);;
  private final Bundle userbundle = new Bundle();

  public void setUp() {
    context = getApplicationContext();
    accountManager = AccountManager.get(context);
    clientsDelegate = new SyncAdapter(context, false);

    ((SyncAdapter)clientsDelegate).localAccount = account;
    accountManager.addAccountExplicitly(account, "", userbundle);
  }

  public void tearDown() {
    // Cleanup.
    clientsDelegate.setClientsCount(0);
  }

  public void testGetAccountGUID() {
    // Test getAccountGUID() saved the value it returned the first time.
    String accountGUID = clientsDelegate.getAccountGUID();
    assertNotNull(accountGUID);
    assertEquals(accountGUID, clientsDelegate.getAccountGUID());
  }

  public void testGetClientName() {
    // Test getClientName() saved the value it returned the first time.
    String clientName = clientsDelegate.getClientName();
    assertNotNull(clientName);
    assertEquals(clientName, clientsDelegate.getClientName());
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
