package org.mozilla.gecko.sync.config.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.mozilla.android.sync.test.AndroidSyncTestCase;
import org.mozilla.android.sync.test.helpers.MockSharedPreferences;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.config.ConfigurationPickler;
import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.setup.SyncAccounts;
import org.mozilla.gecko.sync.setup.SyncAccounts.SyncAccountParameters;
import org.mozilla.gecko.sync.setup.test.TestSyncAccounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;

public class TestConfigurationPickler extends AndroidSyncTestCase {
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

  public static SharedPreferences getPrefs() {
    final SharedPreferences prefs = new MockSharedPreferences();
    prefs.edit().putString("test1", "test").putBoolean("test2", true).putLong("test3", 111L).commit();

    return prefs;
  }

  public static void assertObject(final ExtendedJSONObject o) {
    assertEquals("test", o.get("test1"));
    assertEquals(new Boolean(true), o.get("test2"));
    assertEquals(new Long(111), o.get("test3"));
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

  public void testPickle() {
    final SharedPreferences prefs = getPrefs();
    final ExtendedJSONObject o = ConfigurationPickler.asJSON(prefs);

    assertObject(o);
    assertEquals(3, o.keySet().size());
  }

  public void testPersist() throws Exception {
    final Context context = getApplicationContext();

    context.deleteFile(TEST_FILENAME);
    assertFileNotPresent(context, TEST_FILENAME);

    final SharedPreferences prefs = getPrefs();
    ConfigurationPickler.pickle(context, prefs, TEST_FILENAME, TEST_USERNAME, TEST_PASSWORD, TEST_SERVERURL, TEST_SYNCKEY);

    final String s = Utils.readFile(context, TEST_FILENAME);
    assertNotNull(s);

    final ExtendedJSONObject o = ExtendedJSONObject.parseJSONObject(s);
    assertObject(o);
    assertEquals(3 + 5, o.keySet().size()); // username, password, serverURL, syncKey, timestamp.
  }

  public void testDeletePickle() throws Exception {
    final Context context = getApplicationContext();
    final SharedPreferences prefs = getPrefs();
    ConfigurationPickler.pickle(context, prefs, TEST_FILENAME, TEST_USERNAME, TEST_PASSWORD, TEST_SERVERURL, TEST_SYNCKEY);

    // Verify file is present.
    final String s = Utils.readFile(context, TEST_FILENAME);
    assertNotNull(s);
    assertTrue(s.length() > 0);

    ConfigurationPickler.deletePickle(context, TEST_FILENAME);
    assertFileNotPresent(context, TEST_FILENAME);
  }

  public void testJSONPrefs() {
    final ExtendedJSONObject o = new ExtendedJSONObject();
    o.put("test1", "test");
    o.put("test2", new Boolean(true));
    o.put("test3", new Float(1.5f));
    o.put("test4", new Long(-2L));

    final SharedPreferences prefs = new ConfigurationPickler.JSONPrefs(o);

    assertEquals("test", prefs.getString("test1", null));
    assertEquals(true, prefs.getBoolean("test2", false));
    assertEquals(1.5f, prefs.getFloat("test3", 0.0f));
    assertEquals(-2L, prefs.getLong("test4", 0L));

    Set<String> set = new HashSet<String>();
    set.add("test1");
    set.add("test2");
    set.add("test3");
    set.add("test4");
    assertEquals(set, prefs.getAll().keySet());
  }

  protected Account internalAccount = null;

  public Account unpickle(final Context context, final String filename) {
    internalAccount = null;

    // Unpickle!
    performWait(new Runnable() {
      @Override
      public void run() {
        try {
          runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
              final ConfigurationPickler.UnpickleSyncAccountTask task = new ConfigurationPickler.UnpickleSyncAccountTask() {
                @Override
                public void onPostExecute(Account result) {
                  internalAccount = result;
                  performNotify();
                }
              };

              task.execute(new ConfigurationPickler.UnpickleParameters(context, TEST_FILENAME));
            }
          });
        } catch (Throwable e) {
          performNotify(e);
        }
      }
    });

    return internalAccount;
  }

  public Account deleteAccountsAndUnpickle(final Context context, final AccountManager accountManager, final String filename) {
    Account[] accounts = accountManager.getAccountsByType(TEST_ACCOUNTTYPE);
    for (Account account : accounts) {
      TestSyncAccounts.deleteAccount(this, accountManager, account);
    }

    accounts = accountManager.getAccountsByType(TEST_ACCOUNTTYPE);
    assertEquals(0, accounts.length);

    return unpickle(context, filename);
  }


  public void testUnpickleSuccess() throws Exception {
    final Context context = getApplicationContext();
    final AccountManager accountManager = AccountManager.get(context);

    final SharedPreferences prefs = getPrefs();
    ConfigurationPickler.pickle(context, prefs, TEST_FILENAME, TEST_USERNAME, TEST_PASSWORD, TEST_SERVERURL, TEST_SYNCKEY);

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

      // Verify settings are in place.
      final SharedPreferences accountPrefs = Utils.getSharedPreferences(context, TEST_USERNAME, TEST_SERVERURL);
      assertEquals("test", accountPrefs.getString("test1", null));
      assertEquals(true, accountPrefs.getBoolean("test2", false));
      assertEquals(111L, accountPrefs.getLong("test3", -2L));
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

    final SharedPreferences prefs = getPrefs();
    ConfigurationPickler.pickle(context, prefs, TEST_FILENAME, null, null, TEST_SERVERURL, TEST_SYNCKEY); // Missing username and password.

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

    final SharedPreferences prefs = getPrefs();
    ConfigurationPickler.pickle(context, prefs, TEST_FILENAME, TEST_USERNAME, TEST_PASSWORD, TEST_SERVERURL, TEST_SYNCKEY);

    // Make sure we have no accounts hanging around.
    final Account account = deleteAccountsAndUnpickle(context, accountManager, TEST_FILENAME);
    assertNotNull(account);

    try {
      // Now try again -- should fail since account exists.
      final Account account2 = unpickle(context, TEST_FILENAME);
      assertNull(account2);
    } finally {
      TestSyncAccounts.deleteAccount(this, accountManager, account);
    }
  }
}
