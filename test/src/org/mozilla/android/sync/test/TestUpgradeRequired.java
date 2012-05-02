/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.android.sync.test;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.parser.ParseException;
import org.mozilla.android.sync.test.helpers.MockGlobalSession;
import org.mozilla.android.sync.test.helpers.MockGlobalSessionCallback;
import org.mozilla.android.sync.test.helpers.MockResourceDelegate;
import org.mozilla.android.sync.test.helpers.MockServerSyncStage;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.sync.AlreadySyncingException;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.HTTPFailureException;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncResourceDelegate;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.setup.SyncAccounts;
import org.mozilla.gecko.sync.setup.SyncAccounts.SyncAccountParameters;
import org.mozilla.gecko.sync.stage.GlobalSyncStage;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

import ch.boye.httpclientandroidlib.HttpResponse;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;

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
  private final int     TEST_PORT        = 15325;
  private final String  TEST_SERVER      = "http://localhost:" + TEST_PORT + "/";

  private final String TEST_USERNAME     = "user1";
  private final String TEST_PASSWORD     = "pass1";
  private final String TEST_SYNC_KEY     = "abcdeabcdeabcdeabcdeabcdea";

  private Context context;
  private AccountManager accountManager;
  private Account account;

  // Mock server
  private Container upgradeServer;
  private Connection connection;

  @Override
  public void setUp() {
    context = getApplicationContext();

    // Set up and enable Sync accounts.
    accountManager = AccountManager.get(context);
    SyncAccountParameters syncAccountParams = new SyncAccountParameters(context, accountManager, TEST_USERNAME, TEST_PASSWORD, TEST_SYNC_KEY, TEST_SERVER, null, null, null);
    account = SyncAccounts.createSyncAccount(syncAccountParams);
    SyncAccounts.setSyncAutomatically(account);

    // Create mock server.
    upgradeServer = new Container() {

      @Override
      public void handle(Request request, Response response) {
        Logger.debug(LOG_TAG, "Handling request...");
        try {
          // Default response fields.
          long time = System.currentTimeMillis();
          response.set("Content-Type", "text/plain");
          response.set("Server", "HelloWorld/1.0 (Simple 4.0)");
          response.setDate("Date", time);
          response.setDate("Last-Modified", time);

          final String timestampHeader = Utils.millisecondsToDecimalSecondsString(time);
          response.set("X-Weave-Timestamp", timestampHeader);
          System.out.println("> X-Weave-Timestamp header: " + timestampHeader);

          // HTTP response and response code in body for requiring upgrade.
          response.setCode(400);
          PrintStream bodyStream = response.getPrintStream();
          bodyStream.println("16");
          bodyStream.close();
        } catch (IOException e) {
          fail("Failed on IO error.");
        }
      }
    };
  }

  /**
   * Sync accounts should be disabled when the server responds with a 400
   * response and the "Upgrade Required" response code.
   * @throws CryptoException
   * @throws ParseException
   * @throws IOException
   * @throws NonObjectJSONException
   * @throws IllegalArgumentException
   * @throws SyncConfigurationException
   */
  public void testUpgradeResponse() throws SyncConfigurationException, IllegalArgumentException, NonObjectJSONException, IOException, ParseException, CryptoException {
    final Map<Stage, GlobalSyncStage> stagesToRun = new HashMap<Stage, GlobalSyncStage>();

    // Mock GlobalSession.
    final MockGlobalSessionCallback callback = new MockGlobalSessionCallback();
    final GlobalSession session = new MockGlobalSession(TEST_SERVER, TEST_USERNAME, TEST_PASSWORD,
        new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY), callback) {
      @Override
      protected void prepareStages() {
        super.prepareStages();
        stagesToRun.putAll(this.stages);
        this.stages = stagesToRun;
      }
    };

    // Stage that makes a get() to upgradeServer.
    MockServerSyncStage stage = new MockServerSyncStage(session) {
      @Override
      public void execute() {
        Logger.warn(LOG_TAG, "execute()");
        final WaitHelper innerWaitHelper = new WaitHelper();
        innerWaitHelper.performWait(new Runnable() {
          @Override
          public void run() {
            try {
              Logger.debug(LOG_TAG, "run()");
              final BaseResource r = new BaseResource(TEST_SERVER);
              r.delegate = new MockResourceDelegate(innerWaitHelper) {
                @Override
                public void handleHttpResponse(HttpResponse response) {
                  Logger.warn(LOG_TAG, "handleHttpResponse()");
                  if (response.getStatusLine().getStatusCode() != 200) {
                    session.interpretHTTPFailure(response);
                  }
                  BaseResource.consumeEntity(response);
                  waitHelper.performNotify();
                }
              };
              Logger.debug(LOG_TAG, "get()");
              r.get();
            } catch (URISyntaxException e) {
              innerWaitHelper.performNotify(e);
            }
          }
        });
      }
    };
    stagesToRun.put(Stage.ensureClusterURL, stage);

    startServer(upgradeServer);
    // Run session.
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        try {
          session.start();
        } catch (AlreadySyncingException e) {
          WaitHelper.getTestWaiter().performNotify(e);
          fail(e.toString());
        }
      }
    });
    stopServer();

    assertTrue(callback.calledAborted);
    assertTrue(callback.calledError);
    assertTrue(callback.calledErrorException instanceof HTTPFailureException);

    // 400 error should have occurred.
    SyncStorageResponse httpResponse = ((HTTPFailureException) callback.calledErrorException).response;
    assertEquals(400, httpResponse.getStatusCode());
    try {
      assertEquals("16", httpResponse.body());
    } catch (Exception e) {
      fail("Exception in checking HTTP response body: " + e.toString());
    }

    // Sync accounts should be disabled and have flags.
    Account[] accounts = accountManager.getAccountsByType(Constants.ACCOUNTTYPE_SYNC);
    for (Account a : accounts) {
      assertFalse(ContentResolver.getSyncAutomatically(a, BrowserContract.AUTHORITY));
      assertEquals(0, ContentResolver.getIsSyncable(a, BrowserContract.AUTHORITY));
      assertEquals("1", accountManager.getUserData(a, Constants.DATA_ENABLE_ON_UPGRADE));
    }
  }

  @Override
  public void tearDown() {
    // Delete account.
    accountManager.removeAccount(account, null, null);
  }
  private void startServer(Container server) {
    SyncResourceDelegate.connectionTimeoutInMillis = 1000;
    try {
      Logger.info(LOG_TAG, "Starting HTTP server on port " + TEST_PORT + "...");
      connection = new SocketConnection(server);
      SocketAddress address = new InetSocketAddress(TEST_PORT);
      connection.connect(address);
      Logger.info(LOG_TAG, "Starting HTTP server on port " + TEST_PORT + "... DONE");
    } catch (IOException e) {
      fail("Failed while starting mock server: " + e.toString());
    }
  }

  private void stopServer() {
    Logger.info(LOG_TAG, "Stopping HTTP server on port " + TEST_PORT + "...");
    try {
      if (connection != null) {
        connection.close();
      }
      connection = null;
      Logger.info(LOG_TAG, "Closing connection pool...");
      BaseResource.shutdownConnectionManager();
      Logger.info(LOG_TAG, "Stopping HTTP server on port " + TEST_PORT + "... DONE");
    } catch (IOException ex) {
      Logger.error(LOG_TAG, "Error stopping HTTP server on port " + TEST_PORT + "... DONE", ex);
      fail("Failed while stopping server: " + ex.toString());
    }
  }
}
