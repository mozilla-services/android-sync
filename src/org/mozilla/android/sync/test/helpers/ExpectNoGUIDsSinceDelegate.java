/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.assertEquals;

public class ExpectNoGUIDsSinceDelegate extends DefaultGuidsSinceDelegate {
  
  public void onGuidsSinceSucceeded(String[] guids) {
    AssertionError err = null;
    try {
      assertEquals(0, guids.length);
    } catch (AssertionError e) {
      err = e;
    }
    testWaiter().performNotify(err);
  }
}
