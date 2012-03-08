/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

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

  public static final String LOG_TAG = "WaitHelper";

  public static class Result {
    public Throwable error;
    public Result() {
      error = null;
    }

    public Result(Throwable error) {
      this.error = error;
    }
  }

  public static abstract class WaitHelperError extends Error {
    private static final long serialVersionUID = 7074690961681883619L;
  }

  /**
   * Immutable.
   *
   * @author rnewman
   */
  public static class TimeoutError extends WaitHelperError {
    private static final long serialVersionUID = 8591672555848651736L;
    public final int waitTimeInMillis;

    public TimeoutError(int waitTimeInMillis) {
      this.waitTimeInMillis = waitTimeInMillis;
    }
  }

  public static class MultipleNotificationsError extends WaitHelperError {
    private static final long serialVersionUID = -9072736521571635495L;
  }

  public static class InterruptedError extends WaitHelperError {
    private static final long serialVersionUID = 8383948170038639308L;
  }

  public static class InnerError extends WaitHelperError {
    private static final long serialVersionUID = 3008502618576773778L;
    public Throwable innerError;

    public InnerError(Throwable e) {
      innerError = e;
      if (e != null) {
        // Eclipse prints the stack trace of the cause.
        this.initCause(e);
      }
    }
  }

  public BlockingQueue<Result> queue = new ArrayBlockingQueue<Result>(1);

  /**
   * How long performWait should wait for, in milliseconds, with the
   * convention that a negative value means "wait forever".
   */
  public static int defaultWaitTimeoutInMillis = -1;

  public void info(String message) {
    Log.i(LOG_TAG, message);
  }

  public void warn(String message, Throwable e) {
    Log.w(LOG_TAG, message, e);
  }

  public void performWait(Runnable action) throws WaitHelperError {
    this.performWait(defaultWaitTimeoutInMillis, action);
  }

  public void performWait(int waitTimeoutInMillis, Runnable action) throws WaitHelperError {
    info("performWait called.");

    Result result = null;

    try {
      if (action != null) {
        try {
          action.run();
          Log.i(LOG_TAG, "Action done.");
        } catch (Exception ex) {
          Log.i(LOG_TAG, "Performing action threw: ", ex);
          throw new InnerError(ex);
        }
      }

      if (waitTimeoutInMillis < 0) {
        result = queue.take();
      } else {
        result = queue.poll(waitTimeoutInMillis, TimeUnit.MILLISECONDS);
      }
      Log.i(LOG_TAG, "Got result from queue: " + result);
    } catch (InterruptedException e) {
      // We were interrupted.
      info("performNotify interrupted with InterruptedException " + e);
      final InterruptedError interruptedError = new InterruptedError();
      interruptedError.initCause(e);
      throw interruptedError;
    }

    if (result == null) {
      // We timed out.
      throw new TimeoutError(waitTimeoutInMillis);
    } else if (result.error != null) {
      warn("Notified with error.", result.error);

      // Rethrow any assertion with which we were notified.
      InnerError innerError = new InnerError(result.error);
      throw innerError;
    } else {
      // Success!
    }
  }

  public void performNotify(final Throwable e) {
    if (e != null) {
      warn("performNotify called with Throwable ", e);
    } else {
      info("performNotify called.");
    }

    if (!queue.offer(new Result(e))) {
      // This could happen if performNotify is called multiple times (which is an error).
      throw new MultipleNotificationsError();
    }
  }

  public void performNotify() {
    this.performNotify(null);
  }

  public static Runnable onThreadRunnable(final Runnable r) {
    return new Runnable() {
      @Override
      public void run() {
        new Thread(r).start();
      }
    };
  }

  private static WaitHelper singleWaiter = new WaitHelper();
  public static WaitHelper getTestWaiter() {
    return singleWaiter;
  }

  public static void resetTestWaiter() {
    singleWaiter = new WaitHelper();
  }

  public boolean isIdle() {
    return queue.isEmpty();
  }
}
