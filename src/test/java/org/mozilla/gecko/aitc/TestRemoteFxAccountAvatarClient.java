package org.mozilla.gecko.aitc;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.gecko.browserid.crypto.JWCrypto;
import org.mozilla.gecko.sync.ExtendedJSONObject;

public class TestRemoteFxAccountAvatarClient {
  public static final String TEST_USERNAME = "testx";
  public static final String TEST_AUDIENCE = "https://myapps.mozillalabs.com"; // Default audience accepted by a local dev token server.
  public static final String TEST_TOKEN_SERVER_URL = "https://stage-token.services.mozilla.com";

  protected final String assertion;
  private BlockingFxAccountAvatarClient client;

  public TestRemoteFxAccountAvatarClient() throws Exception {
    assertion = JWCrypto.createMockMyIdAssertion(TEST_USERNAME, TEST_AUDIENCE);
  }

  @Before
  public void setUp() throws Exception {
    client = new BlockingFxAccountAvatarClient(new URI(TEST_TOKEN_SERVER_URL), assertion);
  }

  @Test
  public void testPutGetAvatar() throws Exception {
    ExtendedJSONObject avatar = new ExtendedJSONObject();
    avatar.put("name", "name");
    avatar.put("image", "image");

    client.putAvatar(avatar);

    ExtendedJSONObject fetched = client.getAvatar();

    assertEquals(avatar.toJSONString(), fetched.toJSONString());
  }
}
