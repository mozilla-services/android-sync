/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.AssertionFailedError;

import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.android.sync.test.helpers.WaitHelper.TimeoutError;
import org.mozilla.gecko.sync.StubActivity;
import org.mozilla.gecko.sync.ThreadPool;

import android.test.ActivityInstrumentationTestCase2;

public class WaitHelperTest extends ActivityInstrumentationTestCase2<StubActivity> {
  public static int NO_WAIT = 1; // Milliseconds.
  public static int SHORT_WAIT = 100; // Milliseconds.
  public static int LONG_WAIT = 3*SHORT_WAIT;

  public AtomicBoolean performNotifyCalled = new AtomicBoolean(false);
  public AtomicBoolean performNotifyErrorCalled = new AtomicBoolean(false);
  public WaitHelper waitHelper;

  public WaitHelperTest() {
    super(StubActivity.class);
  }

  public void setUp() {
    WaitHelper.resetTestWaiter();
    waitHelper = WaitHelper.getTestWaiter();

    performNotifyCalled.set(false);
    performNotifyErrorCalled.set(false);
  }

  public Runnable performNothingRunnable() {
    return new Runnable() {
      public void run() {
      }
    };
  }

  public Runnable performNotifyRunnable() {
    return new Runnable() {
      public void run() {
        performNotifyCalled.set(true);
        waitHelper.performNotify();
      }
    };
  }

  public Runnable performNotifyAfterDelayRunnable(final int delayInMillis) {
    return new Runnable() {
      public void run() {
        try {
          Thread.sleep(delayInMillis);
        } catch (InterruptedException e) {
          e.printStackTrace();
          assert(false);
        }

        performNotifyCalled.set(true);
        waitHelper.performNotify();
      }
    };
  }

  public Runnable performNotifyErrorRunnable() {
    return new Runnable() {
      public void run() {
        performNotifyCalled.set(true);
        waitHelper.performNotify(new AssertionError("error unique identifier"));
      }
    };
  }

  public Runnable inThreadPool(final Runnable runnable) {
    return new Runnable() {
      @Override
      public void run() {
        ThreadPool.run(runnable);
      }
    };
  }

  public Runnable inThread(final Runnable runnable) {
    return new Runnable() {
      @Override
      public void run() {
        new Thread(runnable).start();
      }
    };
  }

  public void testPerformWait() {
    waitHelper.performWait(performNotifyRunnable());
    assertTrue(performNotifyCalled.get());
  }

  public void testPerformWaitInThread() {
    waitHelper.performWait(inThread(performNotifyRunnable()));
    assertTrue(performNotifyCalled.get());
  }

  public void testPerformWaitInThreadPool() {
    waitHelper.performWait(inThreadPool(performNotifyRunnable()));
    assertTrue(performNotifyCalled.get());
  }

  public void testPerformTimeoutWait() {
    waitHelper.performWait(SHORT_WAIT, performNotifyRunnable());
    assertTrue(performNotifyCalled.get());
  }

  public void testPerformTimeoutWaitInThread() {
    waitHelper.performWait(SHORT_WAIT, inThread(performNotifyRunnable()));
    assertTrue(performNotifyCalled.get());
  }

  public void testPerformTimeoutWaitInThreadPool() {
    waitHelper.performWait(SHORT_WAIT, inThreadPool(performNotifyRunnable()));
    assertTrue(performNotifyCalled.get());
  }

  public void testPerformErrorWaitInThread() {
    try {
      waitHelper.performWait(inThread(performNotifyErrorRunnable()));
    } catch (AssertionError e) {
      performNotifyErrorCalled.set(true);
      assertTrue("Expected '" + e.getMessage() + "' to contain 'error unique identifer'",
          e.getMessage().contains("error unique identifier"));
    }
    assertTrue(performNotifyCalled.get());
    assertTrue(performNotifyErrorCalled.get());
  }

  public void testPerformErrorWaitInThreadPool() {
    try {
      waitHelper.performWait(inThreadPool(performNotifyErrorRunnable()));
    } catch (AssertionError e) {
      performNotifyErrorCalled.set(true);
      assertTrue("Expected '" + e.getMessage() + "' to contain 'error unique identifer'",
          e.getMessage().contains("error unique identifier"));
    }
    assertTrue(performNotifyCalled.get());
    assertTrue(performNotifyErrorCalled.get());
  }

  public void testPerformErrorTimeoutWaitInThread() {
    try {
      waitHelper.performWait(SHORT_WAIT, inThread(performNotifyErrorRunnable()));
    } catch (AssertionError e) {
      performNotifyErrorCalled.set(true);
      assertTrue("Expected '" + e.getMessage() + "' to contain 'error unique identifer'",
          e.getMessage().contains("error unique identifier"));
    }
    assertTrue(performNotifyCalled.get());
    assertTrue(performNotifyErrorCalled.get());
  }

  public void testPerformErrorTimeoutWaitInThreadPool() {
    try {
      waitHelper.performWait(SHORT_WAIT, inThreadPool(performNotifyErrorRunnable()));
    } catch (AssertionError e) {
      performNotifyErrorCalled.set(true);
      assertTrue("Expected '" + e.getMessage() + "' to contain 'error unique identifer'",
          e.getMessage().contains("error unique identifier"));
    }
    assertTrue(performNotifyCalled.get());
    assertTrue(performNotifyErrorCalled.get());
  }

  public void testTimeout() {
    try {
      waitHelper.performWait(SHORT_WAIT, performNothingRunnable());
    } catch (TimeoutError e) {
      performNotifyErrorCalled.set(true);
      assertEquals(SHORT_WAIT, e.waitTimeInMillis);
    }
    assertTrue(performNotifyErrorCalled.get());
  }

  /**
   * This will pass.  The sequence in the main thread is:
   * - A short delay.
   * - performNotify is called.
   * - performWait is called and immediately finds that performNotify was called before.
   */
  public void testDelay() {
    try {
      waitHelper.performWait(1, performNotifyAfterDelayRunnable(SHORT_WAIT));
    } catch (AssertionError e) {
      performNotifyErrorCalled.set(true);
      assertTrue(e.getMessage(), e.getMessage().contains("TIMEOUT"));
    }
    assertTrue(performNotifyCalled.get());
    assertFalse(performNotifyErrorCalled.get());
  }

  public Runnable performNotifyMultipleTimesRunnable() {
    return new Runnable() {
      public void run() {
        waitHelper.performNotify();
        performNotifyCalled.set(true);
        waitHelper.performNotify();
      }
    };
  }

  public void testPerformNotifyMultipleTimesFails() {
    try {
      waitHelper.performWait(NO_WAIT, performNotifyMultipleTimesRunnable());
    } catch (WaitHelper.MultipleNotificationsError e) {
      performNotifyErrorCalled.set(true);
    }
    assertTrue(performNotifyCalled.get());
    assertTrue(performNotifyErrorCalled.get());
  }

  public void testAssertIsReported() {
    try {
      waitHelper.performWait(1, new Runnable() {
        @Override
        public void run() {
          assertTrue("unique identifier", false);
        }
      });
    } catch (AssertionFailedError e) {
      performNotifyErrorCalled.set(true);
      assertTrue(e.getMessage(), e.getMessage().contains("unique identifier"));
    }
    assertFalse(performNotifyCalled.get());
    assertTrue(performNotifyErrorCalled.get());
  }

  /**
   * This will timeout.  The sequence in the helper thread is:
   * - A short delay.
   * - performNotify is called.
   *
   * The sequence in the main thread is:
   * - performWait is called and times out because the helper thread does not call
   *   performNotify quickly enough.
   */
  public void testDelayInThread() throws InterruptedException {
    try {
      waitHelper.performWait(NO_WAIT, inThread(performNotifyAfterDelayRunnable(SHORT_WAIT)));
    } catch (WaitHelper.TimeoutError e) {
      performNotifyErrorCalled.set(true);
      assertEquals(NO_WAIT, e.waitTimeInMillis);
    }
    assertFalse(performNotifyCalled.get());
    assertTrue(performNotifyErrorCalled.get());

    // The spawned thread should have called performNotify() by now.
    Thread.sleep(LONG_WAIT);
    try {
      waitHelper.performWait(1, this.performNothingRunnable());
    } catch (AssertionError e) {
      fail("Should not have thrown!");
    }
  }

  /**
   * This will timeout.  The sequence in the helper thread is:
   * - A short delay.
   * - performNotify is called.
   *
   * The sequence in the main thread is:
   * - performWait is called and times out because the helper thread does not call
   *   performNotify quickly enough.
   */
  public void testDelayInThreadPool() throws InterruptedException {
    try {
      waitHelper.performWait(NO_WAIT, inThreadPool(performNotifyAfterDelayRunnable(SHORT_WAIT)));
    } catch (WaitHelper.TimeoutError e) {
      performNotifyErrorCalled.set(true);
      assertEquals(NO_WAIT, e.waitTimeInMillis);
    }
    assertFalse(performNotifyCalled.get());
    assertTrue(performNotifyErrorCalled.get());

    // The spawned thread should have called performNotify() by now.
    Thread.sleep(LONG_WAIT);
    try {
      waitHelper.performWait(NO_WAIT, this.performNothingRunnable());
    } catch (AssertionError e) {
      fail("Should not have thrown!");
    }
  }
}
