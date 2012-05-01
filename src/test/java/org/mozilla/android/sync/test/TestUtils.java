/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;
import org.mozilla.gecko.sync.Utils;

public class TestUtils {

  @Test
  public void testGenerateGUID() {
    for (int i = 0; i < 1000; ++i) {
      assertEquals(12, Utils.generateGuid().length());
    }
  }

  @Test
  public void testToCommaSeparatedString() {
    ArrayList<String> xs = new ArrayList<String>();
    assertEquals("", Utils.toCommaSeparatedString(null));
    assertEquals("", Utils.toCommaSeparatedString(xs));
    xs.add("test1");
    assertEquals("test1", Utils.toCommaSeparatedString(xs));
    xs.add("test2");
    assertEquals("test1, test2", Utils.toCommaSeparatedString(xs));
    xs.add("test3");
    assertEquals("test1, test2, test3", Utils.toCommaSeparatedString(xs));
  }
}
