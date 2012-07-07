/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.config.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.mozilla.android.sync.test.AndroidSyncTestCase;
import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.config.AccountPickler;
import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.setup.SyncAccounts.SyncAccountParameters;
import org.mozilla.gecko.sync.setup.test.TestSyncAccounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;

public class TestAccountPickler extends AndroidSyncTestCase {
  public static final String TEST_FILENAME = "test.json";
  public static final String TEST_ACCOUNTTYPE = Constants.ACCOUNTTYPE_SYNC;

  public static final String TEST_USERNAME   = "testAccount@mozilla.com";
  public static final String TEST_SYNCKEY    = "testSyncKey";
  public static final String TEST_PASSWORD   = "testPassword";
  public static final String TEST_SERVERURL  = "test.server.url/";
  public static final String TEST_CLIENT_NAME = "testClientName";
  public static final String TEST_CLIENT_GUID = "testClientGuid";

  protected SyncAccountParameters params;

  public void setUp() {
    params = new SyncAccountParameters(getApplicationContext(), null,
        TEST_USERNAME, TEST_SYNCKEY, TEST_PASSWORD, TEST_SERVERURL, null, TEST_CLIENT_NAME, TEST_CLIENT_GUID);
  }

  public void tearDown() {
    final AccountManager accountManager = AccountManager.get(getApplicationContext());
    final Account[] accounts = accountManager.getAccountsByType(TEST_ACCOUNTTYPE);
    for (Account account : accounts) {
      TestSyncAccounts.deleteAccount(this, accountManager, account);
    }
  }

  public static void assertFileNotPresent(final Context context, final String filename) throws Exception {
    // Verify file is not present.
    FileInputStream fis = null;
    try {
      fis = context.openFileInput(TEST_FILENAME);
      fail("Should get FileNotFoundException.");
    } catch (FileNotFoundException e) {
      // Do nothing; file should not exist.
    } finally {
      if (fis != null) {
        fis.close();
      }
    }
  }

  public void testPersist() throws Exception {
    final Context context = getApplicationContext();

    context.deleteFile(TEST_FILENAME);
    assertFileNotPresent(context, TEST_FILENAME);

    AccountPickler.pickle(context, TEST_FILENAME, params, true);

    final String s = Utils.readFile(context, TEST_FILENAME);
    assertNotNull(s);

    final ExtendedJSONObject o = ExtendedJSONObject.parseJSONObject(s);
    assertEquals(TEST_USERNAME,  o.getString(Constants.JSON_KEY_ACCOUNT));
    assertEquals(TEST_PASSWORD,  o.getString(Constants.JSON_KEY_PASSWORD));
    assertEquals(TEST_SERVERURL, o.getString(Constants.JSON_KEY_SERVER));
    assertEquals(TEST_SYNCKEY,   o.getString(Constants.JSON_KEY_SYNCKEY));
    assertNull(o.getString(Constants.JSON_KEY_CLUSTER));
    assertEquals(TEST_CLIENT_NAME, o.getString(Constants.JSON_KEY_CLIENT_NAME));
    assertEquals(TEST_CLIENT_GUID, o.getString(Constants.JSON_KEY_CLIENT_GUID));
    assertEquals(new Boolean(true), o.get(Constants.JSON_KEY_SYNC_AUTOMATICALLY));
    assertEquals(new Long(AccountPickler.VERSION), o.getLong(Constants.JSON_KEY_VERSION));
    assertTrue(o.containsKey(Constants.JSON_KEY_TIMESTAMP));
  }

  public void testDeletePickle() throws Exception {
    final Context context = getApplicationContext();
    AccountPickler.pickle(context, TEST_FILENAME, params, false);

    // Verify file is present.
    final String s = Utils.readFile(context, TEST_FILENAME);
    assertNotNull(s);
    assertTrue(s.length() > 0);

    AccountPickler.deletePickle(context, TEST_FILENAME);
    assertFileNotPresent(context, TEST_FILENAME);
  }

  public Account deleteAccountsAndUnpickle(final Context context, final AccountManager accountManager, final String filename) {
    Account[] accounts = accountManager.getAccountsByType(TEST_ACCOUNTTYPE);
    for (Account account : accounts) {
      TestSyncAccounts.deleteAccount(this, accountManager, account);
    }

    accounts = accountManager.getAccountsByType(TEST_ACCOUNTTYPE);
    assertEquals(0, accounts.length);

    return AccountPickler.unpickle(context, filename);
  }

  public void testUnpickleSuccess() throws Exception {
    final Context context = getApplicationContext();
    final AccountManager accountManager = AccountManager.get(context);

    AccountPickler.pickle(context, TEST_FILENAME, params, true);

    // Make sure we have no accounts hanging around.
    final Account account = deleteAccountsAndUnpickle(context, accountManager, TEST_FILENAME);
    assertNotNull(account);

    try {
      Account[] accounts = accountManager.getAccountsByType(TEST_ACCOUNTTYPE);
      assertEquals(1, accounts.length);

      assertTrue(ContentResolver.getSyncAutomatically(account, BrowserContract.AUTHORITY));

      assertEquals(account.name, TEST_USERNAME);

      // Verify Account parameters are in place.  Not testing clusterURL since it's stored in
      // shared prefs and it's less critical.
      final String password = accountManager.getPassword(account);
      final String serverURL  = accountManager.getUserData(account, Constants.OPTION_SERVER);
      final String syncKey    = accountManager.getUserData(account, Constants.OPTION_SYNCKEY);
      final String clientName = accountManager.getUserData(account, Constants.CLIENT_NAME);
      final String clientGuid = accountManager.getUserData(account, Constants.ACCOUNT_GUID);

      assertEquals(TEST_PASSWORD, password);
      assertEquals(TEST_SERVERURL, serverURL);
      assertEquals(TEST_SYNCKEY, syncKey);
      assertEquals(TEST_CLIENT_NAME, clientName);
      assertEquals(TEST_CLIENT_GUID, clientGuid);
    } finally {
      TestSyncAccounts.deleteAccount(this, accountManager, account);
    }
  }

  public void testUnpickleNoAutomatic() throws Exception {
    final Context context = getApplicationContext();
    final AccountManager accountManager = AccountManager.get(context);

    AccountPickler.pickle(context, TEST_FILENAME, params, false);

    // Make sure we have no accounts hanging around.
    final Account account = deleteAccountsAndUnpickle(context, accountManager, TEST_FILENAME);
    assertNotNull(account);

    try {
      Account[] accounts = accountManager.getAccountsByType(TEST_ACCOUNTTYPE);
      assertEquals(1, accounts.length);

      assertFalse(ContentResolver.getSyncAutomatically(account, BrowserContract.AUTHORITY));
    } finally {
      TestSyncAccounts.deleteAccount(this, accountManager, account);
    }
  }

  public void testUnpickleNoFile() {
    final Context context = getApplicationContext();
    final AccountManager accountManager = AccountManager.get(context);

    // Just in case file is hanging around.
    context.deleteFile(TEST_FILENAME);

    final Account account = deleteAccountsAndUnpickle(context, accountManager, TEST_FILENAME);
    assertNull(account);
  }

  public void testUnpickleIncompleteUserData() throws Exception {
    final Context context = getApplicationContext();
    final AccountManager accountManager = AccountManager.get(context);

    final FileOutputStream fos = context.openFileOutput(TEST_FILENAME, Context.MODE_PRIVATE);
    final PrintStream ps = (new PrintStream(fos));
    ps.print("{}"); // Valid JSON, just missing everything.
    ps.close();
    fos.close();

    final Account account = deleteAccountsAndUnpickle(context, accountManager, TEST_FILENAME);
    assertNull(account);
  }

  public void testUnpickleMalformedFile() throws Exception {
    final Context context = getApplicationContext();
    final AccountManager accountManager = AccountManager.get(context);

    final FileOutputStream fos = context.openFileOutput(TEST_FILENAME, Context.MODE_PRIVATE);
    final PrintStream ps = (new PrintStream(fos));
    ps.print("{1:!}"); // Not valid JSON.
    ps.close();
    fos.close();

    final Account account = deleteAccountsAndUnpickle(context, accountManager, TEST_FILENAME);
    assertNull(account);
  }

  public void testUnpickleAccountAlreadyExists() {
    final Context context = getApplicationContext();
    final AccountManager accountManager = AccountManager.get(context);

    AccountPickler.pickle(context, TEST_FILENAME, params, false);

    // Make sure we have no accounts hanging around.
    final Account account = deleteAccountsAndUnpickle(context, accountManager, TEST_FILENAME);
    assertNotNull(account);

    // Need to replace file.
    AccountPickler.pickle(context, TEST_FILENAME, params, false);

    try {
      // Now try again -- should fail since account exists.
      final Account account2 = AccountPickler.unpickle(context, TEST_FILENAME);
      assertNull(account2);
    } finally {
      TestSyncAccounts.deleteAccount(this, accountManager, account);
    }
  }
}
