package org.mozilla.android.sync.test.helpers;

import junit.framework.AssertionFailedError;

import org.mozilla.gecko.sync.Logger;

public class ExpectSuccessDelegate {
  public WaitHelper waitHelper;

  public ExpectSuccessDelegate(WaitHelper waitHelper) {
    this.waitHelper = waitHelper;
  }

  public void performNotify() {
    this.waitHelper.performNotify();
  }

  public void performNotify(AssertionFailedError e) {
    this.waitHelper.performNotify(e);
  }

  public String logTag() {
    return this.getClass().getSimpleName();
  }

  public void log(String message) {
    Logger.info(logTag(), message);
  }

  public void log(String message, Throwable throwable) {
    Logger.warn(logTag(), message, throwable);
  }
}