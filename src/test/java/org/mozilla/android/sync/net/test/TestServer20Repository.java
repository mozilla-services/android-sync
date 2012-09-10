package org.mozilla.android.sync.net.test;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.junit.Test;
import org.mozilla.gecko.sync.repositories.Server20Repository;

public class TestServer20Repository {

  private static final String SERVER_URI = "http://server.com/";
  private static final String USERNAME   = "username";
  private static final String COLLECTION = "collection";

  public static void assertQueryEquals(String expected, URI u) {
    assertEquals(expected, u.getRawQuery());
  }

  @Test
  public void testURI() throws Exception {
    final Server20Repository r = new Server20Repository(SERVER_URI, USERNAME, COLLECTION, null);

    assertEquals("http://server.com/2.0/username/storage/collection", r.collectionURI().toString());
  }
}
