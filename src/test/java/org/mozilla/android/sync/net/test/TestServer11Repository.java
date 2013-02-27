/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.net.test;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;
import org.mozilla.gecko.sync.repositories.Server11Repository;

public class TestServer11Repository {

  private static final String SERVER_URI = "http://foo.com/";
  private static final String USERNAME   = "johndoe";
  private static final String COLLECTION = "bookmarks";

  public static void assertQueryEquals(String expected, URI u) {
    assertEquals(expected, u.getRawQuery());
  }

  @SuppressWarnings("static-method")
  @Test
  public void testCollectionURI() throws URISyntaxException {
    Server11Repository r = new Server11Repository(SERVER_URI, USERNAME, COLLECTION, null, null);
    assertQueryEquals("full=1&newer=5000.000",              r.collectionURI(true,  5000000L, -1,    null, null));
    assertQueryEquals("newer=1230.000",                     r.collectionURI(false, 1230000L, -1,    null, null));
    assertQueryEquals("newer=5000.000&limit=10",            r.collectionURI(false, 5000000L, 10,    null, null));
    assertQueryEquals("full=1&newer=5000.000&sort=index",   r.collectionURI(true,  5000000L,  0, "index", null));
    assertQueryEquals("full=1&ids=123,abc",                 r.collectionURI(true,       -1L, -1,    null, "123,abc"));
  }
}
