/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

import java.util.Arrays;

import android.util.Log;

import junit.framework.AssertionFailedError;

public class ExpectGuidsSinceDelegate extends DefaultGuidsSinceDelegate {
  private String[] expected;

  public ExpectGuidsSinceDelegate(String[] guids) {
    expected = guids;
    Arrays.sort(expected);
    Log.i("ExpectGuidsSinceDelegate", "Expecting " + expected.length);
  }

  @Override
  public void onGuidsSinceSucceeded(String[] guids) {
    AssertionFailedError err = null;
    try {
      assertEquals(this.expected.length, guids.length);

      for (String string : guids) {
        assertFalse(-1 == Arrays.binarySearch(this.expected, string));
      }
    } catch (AssertionFailedError e) {
      err = e;
    }
    testWaiter().performNotify(err);
  }
}
