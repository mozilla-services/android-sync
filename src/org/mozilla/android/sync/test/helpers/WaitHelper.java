/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import android.util.Log;

/**
 * Implements waiting for asynchronous test events.
 * @author rnewman
 *
 */
public class WaitHelper {
  AssertionError lastAssertion = null;

  public synchronized void performWait() throws AssertionError {
    Log.i("WaitHelper", "performWait called.");
    try {
      WaitHelper.this.wait();
      // Rethrow any assertion with which we were notified.
      if (this.lastAssertion != null) {
        AssertionError e = this.lastAssertion;
        this.lastAssertion = null;
        throw e;
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public synchronized void performNotify(AssertionError e) {
    if (e != null) {
      Log.i("WaitHelper", "performNotify called with AssertionError " + e);
    }
    this.lastAssertion = e;
    WaitHelper.this.notify();
  }

  public void performNotify() {
    Log.i("WaitHelper", "performNotify called.");
    this.performNotify(null);
  }

  private static WaitHelper singleWaiter;
  public static WaitHelper getTestWaiter() {
    if (singleWaiter == null) {
      singleWaiter = new WaitHelper();
    }
    return singleWaiter;
  }
}
