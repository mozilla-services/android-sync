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
  Object lastAssertionMonitor = new Object();
  AssertionError lastAssertion = null;

  /**
   * We take a Runnable as a parameter so that it'll be invoked inside the
   * synchronized session, which allows us to be waiting by the time the
   * Runnable executes.
   *
   * action *must* start a new thread before attempting to wait or notify
   * this helper, otherwise this trick doesn't work!
   *
   * @param action
   * @throws AssertionError
   */
  public synchronized void performWait(Runnable action) throws AssertionError {
    Log.i("WaitHelper", "performWait called.");
    try {
      if (action != null) {
        try {
          action.run();
        } catch (Exception ex) {
          throw new AssertionError(ex);
        }
      }
      Log.d("WaitHelper", "Waiting.");
      WaitHelper.this.wait();
      synchronized (lastAssertionMonitor) {
        Log.d("WaitHelper", "Done waiting. lastAssertion is " + this.lastAssertion);
        // Rethrow any assertion with which we were notified.
        if (this.lastAssertion != null) {
          AssertionError e = this.lastAssertion;
          this.lastAssertion = null;
          Log.d("WaitHelper", "Rethrowing.", e);
          throw e;
        }
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void performWait() throws AssertionError {
    this.performWait(null);
  }

  public synchronized void performNotify(AssertionError e) {
    if (e != null) {
      Log.i("WaitHelper", "performNotify called with AssertionError " + e);
    }
    synchronized (lastAssertionMonitor) {
      this.lastAssertion = e;
    }
    WaitHelper.this.notify();
  }

  public synchronized void performNotify() {
    Log.i("WaitHelper", "performNotify called.");
    this.performNotify(null);
  }

  private static WaitHelper singleWaiter = new WaitHelper();
  public static WaitHelper getTestWaiter() {
    return singleWaiter;
  }
}
