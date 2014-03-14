/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.tokenserver.test;

import java.net.URI;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mozilla.android.sync.test.integration.IntegrationTestCategory;
import org.mozilla.gecko.background.fxa.FxAccountUtils;
import org.mozilla.gecko.background.testhelpers.WaitHelper;
import org.mozilla.gecko.browserid.BrowserIDKeyPair;
import org.mozilla.gecko.browserid.MockMyIDTokenFactory;
import org.mozilla.gecko.browserid.RSACryptoImplementation;
import org.mozilla.gecko.fxa.FxAccountConstants;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.tokenserver.TokenServerClient;
import org.mozilla.gecko.tokenserver.TokenServerClientDelegate;
import org.mozilla.gecko.tokenserver.TokenServerException;
import org.mozilla.gecko.tokenserver.TokenServerException.TokenServerInvalidCredentialsException;
import org.mozilla.gecko.tokenserver.TokenServerToken;

@Category(IntegrationTestCategory.class)
public class TestLiveTokenServerClient {
  public static final String TEST_USERNAME = "test";

  public static final String TEST_REMOTE_URL = FxAccountConstants.STAGE_TOKEN_SERVER_ENDPOINT;
  public static String TEST_REMOTE_AUDIENCE;

  protected final MockMyIDTokenFactory mockMyIDTokenFactory;
  protected final BrowserIDKeyPair keyPair;

  protected TokenServerClient client;

  public TestLiveTokenServerClient() throws Exception {
    TEST_REMOTE_AUDIENCE = FxAccountUtils.getAudienceForURL(TEST_REMOTE_URL);
    this.mockMyIDTokenFactory = new MockMyIDTokenFactory();
    this.keyPair = RSACryptoImplementation.generateKeyPair(1024);
  }

  @Before
  public void setUp() throws Exception {
    BaseResource.rewriteLocalhost = false;
    client = new TokenServerClient(new URI(TEST_REMOTE_URL), Executors.newSingleThreadExecutor());
  }

  @After
  public void tearDown() {
    BaseResource.rewriteLocalhost = true;
  }

  // Randomized username to avoid server state messing with our flow.
  protected static String getTestUsername() {
    return TEST_USERNAME + System.currentTimeMillis();
  }

  @Test
  public void testTokenServerAllocationConsistency() throws Exception {
    final String stateOne = "abcdefabcdefabcdefabcdefabcdefab";
    final String stateTwo = "ddddefabcdefabcdefabcdefabcdefab";

    final String assertion = mockMyIDTokenFactory.createMockMyIDAssertion(keyPair, getTestUsername(), TEST_REMOTE_AUDIENCE);

    String endpointOne = getEndpoint(assertion, stateOne);
    String endpointTwo = getEndpoint(assertion, stateTwo);
    Assert.assertNotNull(endpointOne);
    Assert.assertNotNull(endpointTwo);
    Assert.assertEquals(endpointTwo, getEndpoint(assertion, stateTwo));
    Assert.assertFalse(endpointOne.equals(endpointTwo));
    boolean failed = false;
    try {
      getEndpoint(assertion, stateOne);
      failed = false;
    } catch (Throwable e) {
      // Don't bother unpacking the exception, but we expect this to consistently fail due to invalid-client-state.
      failed = true;
    }
    Assert.assertTrue(failed);
  }

  private abstract class StringReturningRunnable implements Runnable {
    public String returnValue = null;
  }

  private String getEndpoint(final String assertion, final String state) {
    StringReturningRunnable runnable = new StringReturningRunnable() {
      @Override
      public void run() {
        client.getTokenFromBrowserIDAssertion(assertion, true, state, new TokenServerClientDelegate() {
          @Override
          public void handleSuccess(TokenServerToken token) {
            try {
              returnValue = token.endpoint;
              WaitHelper.getTestWaiter().performNotify();
            } catch (Throwable t) {
              WaitHelper.getTestWaiter().performNotify(t);
            }
          }

          @Override
          public void handleFailure(TokenServerException e) {
            WaitHelper.getTestWaiter().performNotify(e);
          }

          @Override
          public void handleError(Exception e) {
            WaitHelper.getTestWaiter().performNotify(e);
          }

          @Override
          public void handleBackoff(int backoffSeconds) {
          }

          @Override
          public String getUserAgent() {
            return null;
          }
        });
      }
    };
    WaitHelper.getTestWaiter().performWait(runnable);
    return runnable.returnValue;
  }

  @Test
  public void testRemoteSuccess() throws Exception {
    final String assertion = mockMyIDTokenFactory.createMockMyIDAssertion(keyPair, getTestUsername(), TEST_REMOTE_AUDIENCE);
    final String mockClientState = null;
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.getTokenFromBrowserIDAssertion(assertion, true, mockClientState, new TokenServerClientDelegate() {
          @Override
          public void handleSuccess(TokenServerToken token) {
            try {
              Assert.assertNotNull(token.id);
              Assert.assertNotNull(token.key);
              Assert.assertNotNull(Long.valueOf(token.uid));
              WaitHelper.getTestWaiter().performNotify();
            } catch (Throwable t) {
              WaitHelper.getTestWaiter().performNotify(t);
            }
          }

          @Override
          public void handleFailure(TokenServerException e) {
            WaitHelper.getTestWaiter().performNotify(e);
          }

          @Override
          public void handleError(Exception e) {
            WaitHelper.getTestWaiter().performNotify(e);
          }

          @Override
          public void handleBackoff(int backoffSeconds) {
          }

          @Override
          public String getUserAgent() {
            return null;
          }
        });
      }
    });
  }

  @Test
  public void testRemoteFailure() throws Exception {
    final String badAssertion = mockMyIDTokenFactory.createMockMyIDAssertion(keyPair, getTestUsername(), TEST_REMOTE_AUDIENCE,
        0L, 1L, 2L, 3L);
    final String mockClientState = "abcdefabcdefabcdefabcdefabcdefab";
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.getTokenFromBrowserIDAssertion(badAssertion, false, mockClientState, new TokenServerClientDelegate() {
          @Override
          public void handleSuccess(TokenServerToken token) {
            WaitHelper.getTestWaiter().performNotify(new RuntimeException("Expected failure due to expired assertion."));
          }

          @Override
          public void handleFailure(TokenServerException e) {
            try {
              Assert.assertEquals(TokenServerInvalidCredentialsException.class, e.getClass());
              WaitHelper.getTestWaiter().performNotify();
            } catch (Throwable t) {
              WaitHelper.getTestWaiter().performNotify(t);
            }
          }

          @Override
          public void handleError(Exception e) {
            WaitHelper.getTestWaiter().performNotify(e);
          }

          @Override
          public void handleBackoff(int backoffSeconds) {
          }

          @Override
          public String getUserAgent() {
            return null;
          }
        });
      }
    });
  }
}
