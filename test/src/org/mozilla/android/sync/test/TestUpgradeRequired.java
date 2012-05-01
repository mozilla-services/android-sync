package org.mozilla.android.sync.test;

import java.net.URI;
import java.net.URISyntaxException;

import org.mozilla.android.sync.test.helpers.HTTPServerTestHelper;
import org.mozilla.android.sync.test.helpers.MockServer;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.setup.SyncAccounts;
import org.mozilla.gecko.sync.setup.SyncAccounts.SyncAccountParameters;
import org.mozilla.gecko.sync.stage.EnsureClusterURLStage;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import ch.boye.httpclientandroidlib.HttpResponse;

/**
 * When syncing and a server responds with a 400 "Upgrade Required," Sync
 * accounts should be disabled.
 *
 * (We are not testing for package updating, because MY_PACKAGE_REPLACED
 * broadcasts can only be sent by the system. Testing for package replacement
 * needs to be done manually on a device.)
 *
 * @author liuche
 *
 */
public class TestUpgradeRequired extends AndroidSyncTestCase {
  private static final int     TEST_PORT        = 15325;
  private static final String  TEST_SERVER      = "http://localhost:" + TEST_PORT + "/";
  private static final String  TEST_NW_URL      = TEST_SERVER + "/1.0/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/node/weave";
  private HTTPServerTestHelper data             = new HTTPServerTestHelper(TEST_PORT);

  private final String TEST_USERNAME1           = "user1";
  private final String TEST_PASSWORD1           = "pass1";
  private final String TEST_SYNC_KEY1           = "abcdeabcdeabcdeabcdeabcdea";

  private Context context;
  private AccountManager accountManager;
  private Account account;

  @Override
  public void setUp() {
    context = getApplicationContext();

    // Set up and enable Sync accounts.
    accountManager = AccountManager.get(context);
    SyncAccountParameters syncAccountParams = new SyncAccountParameters(context, accountManager, TEST_USERNAME1, TEST_PASSWORD1, TEST_SYNC_KEY1, TEST_SERVER, null, null, null);
    account = SyncAccounts.createSyncAccount(syncAccountParams);
    SyncAccounts.setSyncAutomatically(account);
  }

  public class MockClusterURLFetchDelegate implements EnsureClusterURLStage.ClusterURLFetchDelegate {
    public boolean      failureCalled   = false;
    public HttpResponse failureResponse = null;

    @Override
    public void handleSuccess(URI url) {
      WaitHelper.getTestWaiter().performNotify();
    }

    @Override
    public void handleThrottled() {
      WaitHelper.getTestWaiter().performNotify();
    }

    @Override
    public void handleFailure(HttpResponse response) {
      failureCalled = true;
      failureResponse = response;
      WaitHelper.getTestWaiter().performNotify();
    }

    @Override
    public void handleError(Exception e) {
      WaitHelper.getTestWaiter().performNotify(e);
    }
  }

  public MockClusterURLFetchDelegate doFetchClusterURL() {
    final MockClusterURLFetchDelegate delegate = new MockClusterURLFetchDelegate();
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        try {
          EnsureClusterURLStage.fetchClusterURL(TEST_NW_URL, delegate, new Account[] { account });
        } catch (URISyntaxException e) {
          WaitHelper.getTestWaiter().performNotify(e);
        }
      }
    });
    return delegate;
  }
  /**
   * Sync accounts should be disabled when the server responds with a 400
   * response and the "Upgrade Required" response code.
   */
  public void testUpgradeResponse() {
    // Use server that responds with "Upgrade Required" response code.
    data.startHTTPServer(new MockServer(400, "16"));
    MockClusterURLFetchDelegate delegate = doFetchClusterURL();
    data.stopHTTPServer();

    // 400 error should have occurred.
    assertTrue(delegate.failureCalled);
    assertEquals(400, delegate.failureResponse.getStatusLine().getStatusCode());

    // Sync accounts should be disabled.
    Account[] accounts = accountManager.getAccountsByType(Constants.ACCOUNTTYPE_SYNC);
    for (Account a : accounts) {
      assertFalse(ContentResolver.getSyncAutomatically(a, BrowserContract.AUTHORITY));
      assertEquals(0, ContentResolver.getIsSyncable(a, BrowserContract.AUTHORITY));
    }
    // Delete account.
    accountManager.removeAccount(account, null, null);
  }
}
