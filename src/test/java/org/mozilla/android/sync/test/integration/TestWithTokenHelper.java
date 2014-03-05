/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.integration;

import java.net.URI;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.mozilla.gecko.background.fxa.FxAccountUtils;
import org.mozilla.gecko.background.fxa.SkewHandler;
import org.mozilla.gecko.background.testhelpers.WaitHelper;
import org.mozilla.gecko.browserid.BrowserIDKeyPair;
import org.mozilla.gecko.browserid.MockMyIDTokenFactory;
import org.mozilla.gecko.browserid.RSACryptoImplementation;
import org.mozilla.gecko.fxa.FxAccountConstants;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.HawkAuthHeaderProvider;
import org.mozilla.gecko.tokenserver.TokenServerClient;
import org.mozilla.gecko.tokenserver.TokenServerClientDelegate;
import org.mozilla.gecko.tokenserver.TokenServerException;
import org.mozilla.gecko.tokenserver.TokenServerToken;

/**
 * Helper class to make it easier to test against services requiring a token
 * server token.
 */
public abstract class TestWithTokenHelper {
  protected AuthHeaderProvider authHeaderProvider;
  protected TokenServerToken token;

  @Before
  public void setUp() throws Exception {
    if (token == null) {
      token = TestWithTokenHelper.getTokenBlocking("test");
    }
    final SkewHandler tokenServerSkewHandler = SkewHandler.getSkewHandlerFromEndpointString(token.endpoint);
    final long tokenServerSkew = tokenServerSkewHandler.getSkewInSeconds();
    authHeaderProvider = new HawkAuthHeaderProvider(token.id, token.key.getBytes("UTF-8"), false, tokenServerSkew);
  }

  public static TokenServerToken getTokenBlocking(String mockMyIdUsername) throws Exception {
    String TEST_TOKEN_SERVER_URL = FxAccountConstants.DEFAULT_TOKEN_SERVER_ENDPOINT;
    String TEST_AUDIENCE = FxAccountUtils.getAudienceForURL(TEST_TOKEN_SERVER_URL);
    BrowserIDKeyPair keyPair = RSACryptoImplementation.generateKeyPair(1024);
    final String assertion = new MockMyIDTokenFactory().createMockMyIDAssertion(keyPair, mockMyIdUsername, TEST_AUDIENCE);
    final TokenServerClient tokenServerClient = new TokenServerClient(new URI(TEST_TOKEN_SERVER_URL), Executors.newSingleThreadExecutor());
    final TokenServerToken[] tokens = new TokenServerToken[1];
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        tokenServerClient.getTokenFromBrowserIDAssertion(assertion, true, null, new TokenServerClientDelegate() {
          @Override
          public void handleSuccess(TokenServerToken token) {
            tokens[0] = token;
            WaitHelper.getTestWaiter().performNotify();
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
        });
      }
    });
    return tokens[0];
  }
}
