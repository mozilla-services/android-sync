package org.mozilla.gecko.user.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.gecko.sync.net.BaseResource;

public class TestUserClient {
  public static final String TEST_SERVER_URL = "https://stage-auth.services.mozilla.com";
  public static final String TEST_CLUSTER_URL = "https://stage-sync45.services.mozilla.com/";

  // public static final String TEST_SERVER_URL = "http://localhost:5001";
  // public static final String TEST_CLUSTER_URL = "http://localhost:5001";

  protected BlockingUserClient client;

  @Before
  public void setUp() throws URISyntaxException {
    BaseResource.rewriteLocalhost = false;

    client = new BlockingUserClient(new URI(TEST_SERVER_URL));
  }

  @Test
  public void testIsAvailable() throws Exception {
    String email = "test" + System.currentTimeMillis() + "@test.com";

    assertTrue(client.isAvailable(email));
  }

  @Test
  public void testCreateAccount() throws Exception {
    String email = "test" + System.currentTimeMillis() + "@test.com";
    String password = "test123456789test";

    assertTrue(client.createAccount(email, password));

    assertFalse(client.isAvailable(email));
    assertFalse(client.createAccount(email, password));
  }

  @Test
  public void testGetNode() throws Exception {
    String email = "test123457689test_node_assignment@test.com";
    String password = "test123456789test";

    if (client.isAvailable(email)) {
      assertTrue(client.createAccount(email, password));
    }

    String node = client.getNode(email);
    assertNotNull(node);

    assertEquals(TEST_CLUSTER_URL, node);
  }
}
