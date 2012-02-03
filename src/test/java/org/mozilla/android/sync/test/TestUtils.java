/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.gecko.sync.Utils;

public class TestUtils {

  @Test
  public void testGenerateGUID() {
    for (int i = 0; i < 1000; ++i) {
      assertEquals(12, Utils.generateGuid().length());
    }
  }
}
