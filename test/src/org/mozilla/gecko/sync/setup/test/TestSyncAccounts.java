/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.setup.test;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import org.mozilla.android.sync.test.AndroidSyncTestCase;
import org.mozilla.gecko.sync.GlobalConstants;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.setup.SyncAccounts;
import org.mozilla.gecko.sync.setup.SyncAccounts.SyncAccountParameters;
import org.mozilla.gecko.sync.syncadapter.SyncAdapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * We can use <code>performWait</code> and <code>performNotify</code> here if we
 * are careful about threading issues with <code>AsyncTask</code>. We need to
 * take some care to both create and run certain tasks on the main thread --
 * moving the object allocation out of the UI thread causes failures!
 * <p>
 * @see <a href="http://stackoverflow.com/questions/2321829/android-asynctask-testing-problem-with-android-test-framework">http://stackoverflow.com/questions/2321829/android-asynctask-testing-problem-with-android-test-framework</a>.
 */
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
    performWait(new Runnable() {
      @Override
      public void run() {
        try {
          runTestOnUiThread(new Runnable() {
            final AccountManagerCallback<Boolean> callback = new AccountManagerCallback<Boolean>() {
              @Override
              public void run(AccountManagerFuture<Boolean> future) {
                try {
                  future.getResult(5L, TimeUnit.SECONDS);
                } catch (Exception e) {
                }
                performNotify();
              }
            };

            @Override
            public void run() {
              accountManager.removeAccount(account, callback, null);
            }
          });
        } catch (Throwable e) {
          performNotify(e);
        }
      }
    });
  }

  public void tearDown() {
    if (account == null) {
      return;
    }
    deleteAccount(account);
    account = null;
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
    performWait(new Runnable() {
      @Override
      public void run() {
        try {
          runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
              final SyncAccounts.AccountsExistTask task = new SyncAccounts.AccountsExistTask() {
                @Override
                public void onPostExecute(Boolean result) {
                  self.result = result;
                  performNotify();
                }
              };

              task.execute(context);
            }
          });
        } catch (Throwable e) {
          performNotify(e);
        }
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
    performWait(new Runnable() {
      @Override
      public void run() {
        try {
          runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
              final SyncAccounts.CreateSyncAccountTask task = new SyncAccounts.CreateSyncAccountTask() {
                @Override
                public void onPostExecute(Account account) {
                  self.account = account;
                  performNotify();
                }
              };

              task.execute(syncAccount);
            }
          });
        } catch (Throwable e) {
          performNotify(e);
        }
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

  public void testClientRecord() throws NoSuchAlgorithmException, UnsupportedEncodingException {
    final String TEST_NAME = "testName";
    final String TEST_GUID = "testGuid";
    syncAccount = new SyncAccountParameters(context, null,
        TEST_USERNAME, TEST_SYNCKEY, TEST_PASSWORD, null, null, TEST_NAME, TEST_GUID);
    account = SyncAccounts.createSyncAccount(syncAccount);
    assertNotNull(account);

    SyncAdapter syncAdapter = new SyncAdapter(context, false);
    syncAdapter.localAccount = account;

    assertEquals(TEST_GUID, syncAdapter.getAccountGUID());
    assertEquals(TEST_NAME, syncAdapter.getClientName());

    // Let's verify that clusterURL is correctly not set.
    SharedPreferences prefs = Utils.getSharedPreferences(context, TEST_USERNAME, SyncAccounts.DEFAULT_SERVER);
    String clusterURL = prefs.getString(SyncConfiguration.PREF_CLUSTER_URL, null);
    assertNull(clusterURL);
  }

  public void testClusterURL() throws NoSuchAlgorithmException, UnsupportedEncodingException {
    final String TEST_SERVER_URL = "test.server.url/";
    final String TEST_CLUSTER_URL = "test.cluster.url/";
    syncAccount = new SyncAccountParameters(context, null,
        TEST_USERNAME, TEST_SYNCKEY, TEST_PASSWORD, TEST_SERVER_URL, TEST_CLUSTER_URL, null, null);
    account = SyncAccounts.createSyncAccount(syncAccount);
    assertNotNull(account);

    SharedPreferences prefs = Utils.getSharedPreferences(context, TEST_USERNAME, TEST_SERVER_URL);
    String clusterURL = prefs.getString(SyncConfiguration.PREF_CLUSTER_URL, null);
    assertNotNull(clusterURL);
    assertEquals(TEST_CLUSTER_URL, clusterURL);

    SyncAdapter syncAdapter = new SyncAdapter(context, false);
    syncAdapter.localAccount = account;

    // Let's verify that client name and GUID are not set (and the default is returned).
    String guid = syncAdapter.getAccountGUID();
    assertNotNull(guid);
    String name = syncAdapter.getClientName();
    assertNotNull(name);
    assertTrue(name.startsWith(GlobalConstants.PRODUCT_NAME));
  }
}
