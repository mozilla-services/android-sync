/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.setup.activities.test;

import org.mozilla.android.sync.test.AndroidSyncTestCase;
import org.mozilla.gecko.sync.setup.activities.WebURLFinder;

/**
 * These tests are on device because the WebKit APIs are stubs on desktop.
 */
public class TestWebURLFinder extends AndroidSyncTestCase {
  public String find(String text) {
    return new WebURLFinder(text).bestWebURL();
  }

  public void testNoEmail() {
    assertNull(find("test@test.com"));
  }

  public void testSchemeFirst() {
    assertEquals("http://scheme.com", find("test.com http://scheme.com"));
  }

  public void testNoScheme() {
    assertEquals("noscheme.com", find("noscheme.com"));
  }

  public void testNoBadScheme() {
    assertEquals(null, find("file:///test javascript:///test.js"));
  }
}
