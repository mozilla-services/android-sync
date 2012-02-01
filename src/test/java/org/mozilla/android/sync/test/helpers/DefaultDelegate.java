/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.fail;

import java.util.concurrent.ExecutorService;

public abstract class DefaultDelegate {

  protected ExecutorService executor;

  protected WaitHelper testWaiter() {
    return WaitHelper.getTestWaiter();
  }

  protected void sharedFail(String message) {
    try {
      fail(message);
    } catch (AssertionError e) {
      testWaiter().performNotify(e);
    }
  }

}
