/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import junit.framework.AssertionFailedError;

import android.util.Log;

/**
 * Implements waiting for asynchronous test events.
 *
 * Call WaitHelper.getTestWaiter() to get the unique instance.
 *
 * Call performWait(runnable) to execute runnable synchronously.
 * runnable *must* call performNotify() on all exit paths to signal to
 * the TestWaiter that the runnable has completed.
 *
 * @author rnewman
 * @author nalexander
 */
public class WaitHelper {

  public class Result {
    public AssertionError error;
    public Result() {
      error = null;
    }

    public Result(AssertionError error) {
      this.error = error;
    }
  }

  public class TimeoutError extends AssertionError {
    private static final long serialVersionUID = 8591672555848651736L;
    public int waitTimeInMillis = -1;

    public TimeoutError(int waitTimeInMillis) {
      this.waitTimeInMillis = waitTimeInMillis;
    }
  }

  public class MultipleNotificationsError extends AssertionError {
    private static final long serialVersionUID = -9072736521571635495L;
  }

  public BlockingQueue<Result> queue = new ArrayBlockingQueue<Result>(1);

  public static final String LOG_TAG = "WaitHelper";

  /**
   * How long performWait should wait for, in milliseconds, with the
   * convention that a negative value means "wait forever".
   */
  public static int defaultWaitTimeoutInMillis = -1;

  public void trace(String message) {
    Log.i(LOG_TAG, message);
  }

  public void performWait(Runnable action) throws AssertionError {
    this.performWait(defaultWaitTimeoutInMillis, action);
  }

  public void performWait(int waitTimeoutInMillis, Runnable action) throws AssertionError {
    trace("performWait called.");

    Result result = null;

    try {
      if (action != null) {
        try {
          action.run();
        } catch (Exception ex) {
          throw new AssertionError(ex);
        }
      }

      if (waitTimeoutInMillis < 0) {
        result = queue.take();
      } else {
        result = queue.poll(waitTimeoutInMillis, TimeUnit.MILLISECONDS);
      }
    } catch (InterruptedException e) {
      // We were interrupted.
      trace("performNotify interrupted with InterrupedException " + e);
      throw new AssertionError("INTERRUPTED!");
    }

    if (result == null) {
      // We timed out.
      throw new TimeoutError(waitTimeoutInMillis);
    } else if (result.error != null) {
      // Rethrow any assertion with which we were notified.
      throw new AssertionError(result.error);
    } else {
      // Success!
    }
  }

  public void performNotify(final AssertionError e) {
    if (e != null) {
      trace("performNotify called with AssertionError " + e);
    } else {
      trace("performNotify called.");
    }

    if (!queue.offer(new Result(e))) {
      // This could happen if performNotify is called multiple times (which is an error).
      throw new MultipleNotificationsError();
    }
  }

  public void performNotify(final AssertionFailedError e) {
    AssertionError ex = null;

    if (e != null) {
      ex = new AssertionError(e);
    }

    this.performNotify(ex);
  }

  public void performNotify() {
    this.performNotify((AssertionError)null);
  }

  private static WaitHelper singleWaiter = new WaitHelper();
  public static WaitHelper getTestWaiter() {
    return singleWaiter;
  }

  public static void resetTestWaiter() {
    singleWaiter = new WaitHelper();
  }
}
