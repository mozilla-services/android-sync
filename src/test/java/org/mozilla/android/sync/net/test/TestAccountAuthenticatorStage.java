package org.mozilla.android.sync.net.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.HTTPServerTestHelper;
import org.mozilla.android.sync.test.helpers.MockServer;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.android.sync.test.helpers.WaitHelper.InnerError;
import org.mozilla.gecko.sync.setup.auth.AuthenticateAccountStage;
import org.mozilla.gecko.sync.setup.auth.AuthenticateAccountStage.AuthenticateAccountStageDelegate;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import ch.boye.httpclientandroidlib.HttpResponse;

/**
 * Tests the authentication request stage of manual Account setup.
 * @author liuche
 *
 */
public class TestAccountAuthenticatorStage {
  private final String LOG_TAG = "TestAcctAuthStage";

  private static final int TEST_PORT      = 15325;
  private static final String TEST_SERVER = "http://localhost:" + TEST_PORT;

  private static final String USERNAME  = "john-hashed";
  private static final String PASSWORD  = "password";

  private MockServer authServer;
  private HTTPServerTestHelper serverHelper = new HTTPServerTestHelper(TEST_PORT);
  private AuthenticateAccountStage authStage = new AuthenticateAccountStage();
  private AuthenticateAccountStageDelegate testCallback;

  @Before
  public void setup() {
    // Make mock server to check authentication header.
    authServer = new MockServer() {
      @Override
      protected void handle(Request request, Response response, int code, String body) {
        try {
          String responseAuth = request.getValue("Authorization");
          // Trim whitespace, HttpResponse has an extra space?
          if (expectedBasicAuthHeader.equals(responseAuth.trim())) {
            code = 200;
          } else {
            code = 401;
          }

          System.out.println("Handling request...");
          PrintStream bodyStream = this.handleBasicHeaders(request, response, code, "application/json");
          bodyStream.println(body);
          bodyStream.close();
        } catch (IOException e) {
          System.err.println("Oops.");
        }
      }
    };
    authServer.expectedBasicAuthHeader = authStage.makeAuthHeader(USERNAME, PASSWORD);
    System.out.println("expected: {" + authServer.expectedBasicAuthHeader + "}");

    // Authentication delegate to handle HTTP responses.
    testCallback = new AuthenticateAccountStageDelegate() {
      protected int numFailedTries = 0;

      @Override
      public void handleSuccess(boolean isSuccess) {
        if (isSuccess) {
          // Succeed on retry (after failing first attempt).
          assertEquals(1, numFailedTries);
          testWaiter().performNotify();
        } else {
          numFailedTries++;
          // Fail only once.
          if (numFailedTries != 1) {
            testWaiter().performNotify(new Exception("Failed on retry."));
            return;
          }

          // Failed on first try as expected, retry request with correct
          // credentials.
          try {
          testWaiter().performWait(new Runnable() {
            @Override
            public void run() {
              String authHeader = authStage.makeAuthHeader(USERNAME, PASSWORD);
              try {
                authStage.authenticateAccount(testCallback, TEST_SERVER, authHeader);
              } catch (URISyntaxException e) {
                fail("Malformed URI.");
              }
            }
          });
          } catch (InnerError e) {
            fail("testWaiter inner error.");
          }
        }
      }

      @Override
      public void handleFailure(HttpResponse response) {
        fail("Unexpected response " + response.getStatusLine().getStatusCode());
      }

      @Override
      public void handleError(Exception e) {
        System.out.println("handleError()");
        fail("Unexpected error during authentication.");
      }
    };
  }

  @After
  public void cleanup() {
    serverHelper.stopHTTPServer();
  }

  @Test
  public void testAuthenticationRetry() {
    serverHelper.startHTTPServer(authServer);
    testWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        // Try auth request with incorrect password. We want to fail the first time.
        String authHeader = authStage.makeAuthHeader(USERNAME, "wrong-password");
        try {
          authStage.authenticateAccount(testCallback, TEST_SERVER, authHeader);
        } catch (URISyntaxException e) {
          fail("Malformed URI.");
        }
      }
    });
  }

  protected WaitHelper testWaiter() {
    return WaitHelper.getTestWaiter();
  }
}
