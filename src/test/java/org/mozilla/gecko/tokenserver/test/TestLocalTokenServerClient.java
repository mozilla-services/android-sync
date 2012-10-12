package org.mozilla.gecko.tokenserver.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.net.URI;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.browserid.crypto.JWCrypto;
import org.mozilla.gecko.browserid.crypto.test.BlockingBrowserIDVerifierClient;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.tokenserver.TokenServerException.TokenServerInvalidCredentialsException;
import org.mozilla.gecko.tokenserver.TokenServerToken;

public class TestLocalTokenServerClient {
  public static final String TEST_USERNAME = "test";

  public static final String TEST_LOCAL_SERVER_URL = "http://localhost:5000";
  public static final String TEST_LOCAL_AUDIENCE = "http://localhost:5000"; // Default audience accepted by a local dev token server.
  public static final String TEST_LOCAL_URL = TEST_LOCAL_SERVER_URL + "/1.0/sync/2.0";

  protected BlockingBrowserIDVerifierClient verifierClient;
  protected BlockingTokenServerClient blockingClient;

  @Before
  public void setUp() throws Exception {
    BaseResource.rewriteLocalhost = false;

    this.verifierClient = new BlockingBrowserIDVerifierClient();
  }

  @After
  public void tearDown() {
    BaseResource.rewriteLocalhost = true;
  }

  @Test
  public void testLocalSuccess() throws Exception {
    String assertion = JWCrypto.createMockMyIdAssertion(TEST_USERNAME, TEST_LOCAL_AUDIENCE);

    this.verifierClient.assertVerifySuccess(TEST_LOCAL_AUDIENCE, assertion);

    blockingClient = new BlockingTokenServerClient(new URI(TEST_LOCAL_URL));
    TokenServerToken token = blockingClient.getTokenFromBrowserIDAssertion(assertion, true);

    assertEquals("http://localhost:5000/service/sync/2.0/" + token.uid, token.endpoint);
  }

  @Test
  public void testLocalFailure() throws Exception {
    String badAssertion = JWCrypto.createMockMyIdAssertion(TEST_USERNAME, TEST_LOCAL_AUDIENCE, 0, 1);

    this.verifierClient.assertVerifyFailure(TEST_LOCAL_AUDIENCE, badAssertion, "assertion has expired");

    try {
      blockingClient = new BlockingTokenServerClient(new URI(TEST_LOCAL_URL));
      TokenServerToken token = blockingClient.getTokenFromBrowserIDAssertion(badAssertion, false);

      assertNull(token);

      fail("Expected exception.");
    } catch (WaitHelper.InnerError e) {
      assertEquals(TokenServerInvalidCredentialsException.class, e.innerError.getClass());
    }
  }
}
