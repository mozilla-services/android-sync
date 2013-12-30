/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.tokenserver.test;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mozilla.android.sync.test.integration.IntegrationTestCategory;
import org.mozilla.gecko.background.testhelpers.WaitHelper;
import org.mozilla.gecko.browserid.BrowserIDKeyPair;
import org.mozilla.gecko.browserid.JSONWebTokenUtils;
import org.mozilla.gecko.browserid.MockMyIDTokenFactory;
import org.mozilla.gecko.browserid.RSACryptoImplementation;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.tokenserver.TokenServerClient;
import org.mozilla.gecko.tokenserver.TokenServerClientDelegate;
import org.mozilla.gecko.tokenserver.TokenServerException;
import org.mozilla.gecko.tokenserver.TokenServerException.TokenServerInvalidCredentialsException;
import org.mozilla.gecko.tokenserver.TokenServerToken;

@Category(IntegrationTestCategory.class)
public class TestLiveTokenServerClient {
  public static final String TEST_USERNAME = "test";

  public static final String TEST_REMOTE_SERVER_URL = "http://auth.oldsync.dev.lcip.org";
  public static final String TEST_REMOTE_AUDIENCE = "http://auth.oldsync.dev.lcip.org"; // Audience accepted by the token server.
  public static final String TEST_REMOTE_URL = TEST_REMOTE_SERVER_URL + "/1.0/sync/1.1";
  public static final String TEST_ENDPOINT = "http://db1.oldsync.dev.lcip.org/1.1/";

  protected final MockMyIDTokenFactory mockMyIDTokenFactory;
  protected final BrowserIDKeyPair keyPair;

  protected TokenServerClient client;

  public TestLiveTokenServerClient() throws NoSuchAlgorithmException {
    this.mockMyIDTokenFactory = new MockMyIDTokenFactory();
    this.keyPair = RSACryptoImplementation.generateKeypair(1024);
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

  @Test
  public void testRemoteSuccess() throws Exception {
    final String assertion = mockMyIDTokenFactory.createMockMyIDAssertion(keyPair, TEST_USERNAME, TEST_REMOTE_AUDIENCE);
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.getTokenFromBrowserIDAssertion(assertion, true, new TokenServerClientDelegate() {
          @Override
          public void handleSuccess(TokenServerToken token) {
            try {
              Assert.assertNotNull(token.id);
              Assert.assertNotNull(token.key);
              Assert.assertNotNull(Long.valueOf(token.uid));
              Assert.assertEquals(TEST_ENDPOINT + token.uid, token.endpoint);
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
        });
      }
    });
  }

  @Test
  public void testRemoteFailure() throws Exception {
    final String badAssertion = mockMyIDTokenFactory.createMockMyIDAssertion(keyPair, TEST_USERNAME, TEST_REMOTE_AUDIENCE,
        0, 1,
        System.currentTimeMillis(), JSONWebTokenUtils.DEFAULT_ASSERTION_DURATION_IN_MILLISECONDS);
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.getTokenFromBrowserIDAssertion(badAssertion, false, new TokenServerClientDelegate() {
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
        });
      }
    });
  }
}
