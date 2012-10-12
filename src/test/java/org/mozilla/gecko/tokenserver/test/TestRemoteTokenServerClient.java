package org.mozilla.gecko.tokenserver.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.net.URI;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.gecko.browserid.mockmyid.MockMyIDTokenFactory;
import org.mozilla.gecko.browserid.verifier.test.BlockingBrowserIDVerifierClient;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.tokenserver.BlockingTokenServerClient;
import org.mozilla.gecko.tokenserver.BlockingTokenServerClient.BlockingTokenServerException;
import org.mozilla.gecko.tokenserver.TokenServerException.TokenServerInvalidCredentialsException;
import org.mozilla.gecko.tokenserver.TokenServerToken;

public class TestRemoteTokenServerClient {
  public static final String TEST_USERNAME = "test";

  public static final String TEST_REMOTE_SERVER_URL = "https://stage-token.services.mozilla.com";
  public static final String TEST_REMOTE_AUDIENCE = "https://myapps.mozillalabs.com"; // Default audience accepted by a local dev token server.
  public static final String TEST_REMOTE_URL = TEST_REMOTE_SERVER_URL + "/1.0/aitc/1.0";

  protected MockMyIDTokenFactory mockMyIDTokenFactory;

  protected BlockingBrowserIDVerifierClient verifierClient;
  protected BlockingTokenServerClient blockingClient;

  @Before
  public void setUp() throws Exception {
    BaseResource.rewriteLocalhost = false;

    this.verifierClient = new BlockingBrowserIDVerifierClient();
    this.mockMyIDTokenFactory = new MockMyIDTokenFactory();
  }

  @After
  public void tearDown() {
    BaseResource.rewriteLocalhost = true;
  }

  @Test
  public void testRemoteSuccess() throws Exception {
    String assertion = mockMyIDTokenFactory.createMockMyIDAssertion(TEST_USERNAME, TEST_REMOTE_AUDIENCE);

    this.verifierClient.assertVerifySuccess(TEST_REMOTE_AUDIENCE, assertion);

    blockingClient = new BlockingTokenServerClient(new URI(TEST_REMOTE_URL));
    TokenServerToken token = blockingClient.getTokenFromBrowserIDAssertion(assertion, true);

    // These are okay because the token server should always return the same uid
    // for the same user.
    assertEquals("1659259", token.uid);
    assertEquals("https://stage-aitc1.services.mozilla.com/1.0/1659259", token.endpoint);
  }

  @Test
  public void testRemoteFailure() throws Exception {
    String badAssertion = mockMyIDTokenFactory.createMockMyIDAssertion(TEST_USERNAME, TEST_REMOTE_AUDIENCE, 0, 1);

    this.verifierClient.assertVerifyFailure(TEST_REMOTE_AUDIENCE, badAssertion, "assertion has expired");

    try {
      blockingClient = new BlockingTokenServerClient(new URI(TEST_REMOTE_URL));
      TokenServerToken token = blockingClient.getTokenFromBrowserIDAssertion(badAssertion, false);

      assertNull(token);

      fail("Expected exception.");
    } catch (Exception e) {
      assertEquals(BlockingTokenServerException.class, e.getClass());
      assertEquals(TokenServerInvalidCredentialsException.class, e.getCause().getClass());
    }
  }
}
