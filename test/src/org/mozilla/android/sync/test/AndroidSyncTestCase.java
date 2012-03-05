/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import junit.framework.AssertionFailedError;

import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.StubActivity;
import org.mozilla.gecko.sync.repositories.InactiveSessionException;
import org.mozilla.gecko.sync.repositories.InvalidSessionTransitionException;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;

/**
 * AndroidSyncTestCase provides helper methods for testing.
 */
public class AndroidSyncTestCase extends ActivityInstrumentationTestCase2<StubActivity> {
  protected static String LOG_TAG = "AndroidSyncTestCase";

  public AndroidSyncTestCase() {
    super(StubActivity.class);
  }

  public Context getApplicationContext() {
    return this.getInstrumentation().getTargetContext().getApplicationContext();
  }

  public static void performWait(Runnable runnable) throws AssertionFailedError {
    WaitHelper.getTestWaiter().performWait(runnable);
  }

  public static void performNotify() {
    WaitHelper.getTestWaiter().performNotify();
  }

  public static void performNotify(Throwable e) {
    WaitHelper.getTestWaiter().performNotify(e);
  }

  public static void performNotify(InactiveSessionException e) {
    AssertionFailedError er = new AssertionFailedError("Inactive session.");
    er.initCause(e);
    WaitHelper.getTestWaiter().performNotify(er);
  }
  
  public static void performNotify(InvalidSessionTransitionException e) {
    AssertionFailedError er = new AssertionFailedError("Invalid session transition.");
    er.initCause(e);
    WaitHelper.getTestWaiter().performNotify(er);
  }

  public static void performNotify(String reason, Throwable e) {
    AssertionFailedError er = new AssertionFailedError(reason + ": " + e.getMessage());
    er.initCause(e);
    WaitHelper.getTestWaiter().performNotify(er);
  }
}
