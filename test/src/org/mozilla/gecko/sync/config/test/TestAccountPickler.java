package org.mozilla.gecko.sync.config.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.mozilla.android.sync.test.AndroidSyncTestCase;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.config.AccountPickler;
import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.setup.SyncAccounts;
import org.mozilla.gecko.sync.setup.SyncAccounts.SyncAccountParameters;
import org.mozilla.gecko.sync.setup.test.TestSyncAccounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

public class TestAccountPickler extends AndroidSyncTestCase {
  public static final String TEST_FILENAME = "test.json";
  public static final String TEST_ACCOUNTTYPE = Constants.ACCOUNTTYPE_SYNC;

  public static final String TEST_USERNAME   = "testAccount@mozilla.com";
  public static final String TEST_SYNCKEY    = "testSyncKey";
  public static final String TEST_PASSWORD   = "testPassword";
  public static final String TEST_SERVERURL  = "test.server.url/";

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

    AccountPickler.pickle(context, TEST_FILENAME, TEST_USERNAME, TEST_PASSWORD, TEST_SERVERURL, TEST_SYNCKEY);

    final String s = Utils.readFile(context, TEST_FILENAME);
    assertNotNull(s);

    final ExtendedJSONObject o = ExtendedJSONObject.parseJSONObject(s);
    assertEquals(TEST_USERNAME,  o.getString(Constants.JSON_KEY_ACCOUNT));
    assertEquals(TEST_PASSWORD,  o.getString(Constants.JSON_KEY_PASSWORD));
    assertEquals(TEST_SERVERURL, o.getString(Constants.JSON_KEY_SERVER));
    assertEquals(TEST_SYNCKEY,   o.getString(Constants.JSON_KEY_SYNCKEY));
    assertTrue(o.containsKey(Constants.JSON_KEY_TIMESTAMP));
    assertEquals(5, o.keySet().size()); // username, password, serverURL, syncKey, timestamp.
  }

  public void testDeletePickle() throws Exception {
    final Context context = getApplicationContext();
    AccountPickler.pickle(context, TEST_FILENAME, TEST_USERNAME, TEST_PASSWORD, TEST_SERVERURL, TEST_SYNCKEY);

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

    return AccountPickler.unpickle(context, filename, false);
  }


  public void testUnpickleSuccess() throws Exception {
    final Context context = getApplicationContext();
    final AccountManager accountManager = AccountManager.get(context);

    AccountPickler.pickle(context, TEST_FILENAME, TEST_USERNAME, TEST_PASSWORD, TEST_SERVERURL, TEST_SYNCKEY);

    // Make sure we have no accounts hanging around.
    final Account account = deleteAccountsAndUnpickle(context, accountManager, TEST_FILENAME);
    assertNotNull(account);

    try {
      Account[] accounts = accountManager.getAccountsByType(TEST_ACCOUNTTYPE);
      assertEquals(1, accounts.length);

      assertEquals(account.name, TEST_USERNAME);

      // Verify Account parameters are in place.
      final SyncAccountParameters params = SyncAccounts.blockingFromAndroidAccount(context, accountManager, account, 1);
      assertNotNull(params);
      assertEquals(Utils.usernameFromAccount(TEST_USERNAME), params.username);
      assertEquals(TEST_PASSWORD, params.password);
      assertEquals(TEST_SERVERURL, params.serverURL);
      assertEquals(TEST_SYNCKEY, params.syncKey);
    } finally {
      TestSyncAccounts.deleteAccount(this, accountManager, account);
    }
  }

  public void testUnpickleNoFile() {
    final Context context = getApplicationContext();
    final AccountManager accountManager = AccountManager.get(context);

    context.deleteFile(TEST_FILENAME);
    final Account account = deleteAccountsAndUnpickle(context, accountManager, TEST_FILENAME);
    assertNull(account);
  }

  public void testUnpickleIncompleteUserData() {
    final Context context = getApplicationContext();
    final AccountManager accountManager = AccountManager.get(context);

    AccountPickler.pickle(context, TEST_FILENAME, null, null, TEST_SERVERURL, TEST_SYNCKEY); // Missing username and password.

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

    AccountPickler.pickle(context, TEST_FILENAME, TEST_USERNAME, TEST_PASSWORD, TEST_SERVERURL, TEST_SYNCKEY);

    // Make sure we have no accounts hanging around.
    final Account account = deleteAccountsAndUnpickle(context, accountManager, TEST_FILENAME);
    assertNotNull(account);

    try {
      // Now try again -- should fail since account exists.
      final Account account2 = AccountPickler.unpickle(context, TEST_FILENAME, false);
      assertNull(account2);
    } finally {
      TestSyncAccounts.deleteAccount(this, accountManager, account);
    }
  }
}
