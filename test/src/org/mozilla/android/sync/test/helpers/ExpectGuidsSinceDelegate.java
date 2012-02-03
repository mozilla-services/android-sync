/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

import java.util.Arrays;

public class ExpectGuidsSinceDelegate extends DefaultGuidsSinceDelegate {
  private String[] expected;

  public ExpectGuidsSinceDelegate(String[] guids) {
    expected = guids;
    Arrays.sort(expected);
  }

  @Override
  public void onGuidsSinceSucceeded(String[] guids) {
    AssertionError err = null;
    try {
      assertEquals(guids.length, this.expected.length);

      for (String string : guids) {
        assertFalse(-1 == Arrays.binarySearch(this.expected, string));
      }
    } catch (AssertionError e) {
      err = e;
    }
    testWaiter().performNotify(err);
  }
}
