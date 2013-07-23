/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mozilla.gecko.background.common.GlobalConstants;

public class TestConstants {
  @SuppressWarnings("static-method")
  @Test
  public void testGlobalConstantsSanity() {
    final long sec = GlobalConstants.BUILD_TIMESTAMP_SECONDS;
    final long msec = GlobalConstants.BUILD_TIMESTAMP_MSEC;
    assertEquals(1000L * sec, msec);
    assertTrue(msec <= System.currentTimeMillis());
    assertTrue(msec > 1374606872507L);
  }
}
