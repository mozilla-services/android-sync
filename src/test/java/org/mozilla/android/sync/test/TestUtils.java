/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.Test;
import org.mozilla.gecko.sync.Utils;

public class TestUtils {

  @Test
  public void testFillArraySpaces() throws Exception {
    HashMap<String, Long> from = new HashMap<String, Long>();
    from.put("Foo", 0L);
    from.put("Bar", 2L);
    String[] to = new String[4];
    to[0] = "Baz";
    to[2] = "Noo";
    Utils.fillArraySpaces(to, from);
    assertEquals("Baz", to[0]);
    assertEquals("Foo", to[1]);
    assertEquals("Noo", to[2]);
    assertEquals("Bar", to[3]);
    assertEquals(new Long(1L), from.get("Foo"));
    assertEquals(new Long(3L), from.get("Bar"));

    Utils.fillArraySpaces(new String[] {}, new HashMap<String, Long>());
  }

  @Test
  public void testPack() {
    String[] testArray = new String[] {
        "a", null, "b", null, "c", "d", null
    };
    assertEquals(3, Utils.pack(testArray));
    assertEquals("a", testArray[0]);
    assertEquals("b", testArray[1]);
    assertEquals("c", testArray[2]);
    assertEquals("d", testArray[3]);
    assertNull(testArray[4]);
    assertNull(testArray[5]);
    assertNull(testArray[6]);
  }

  @Test
  public void testGenerateGUID() {
    for (int i = 0; i < 1000; ++i) {
      assertEquals(12, Utils.generateGuid().length());
    }
  }
}
