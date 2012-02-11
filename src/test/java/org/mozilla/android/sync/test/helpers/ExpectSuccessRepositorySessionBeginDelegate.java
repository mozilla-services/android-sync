package org.mozilla.android.sync.test.helpers;

import java.util.concurrent.ExecutorService;

import junit.framework.AssertionFailedError;

import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionBeginDelegate;

public class ExpectSuccessRepositorySessionBeginDelegate
extends ExpectSuccessDelegate
implements RepositorySessionBeginDelegate {

  public ExpectSuccessRepositorySessionBeginDelegate(WaitHelper waitHelper) {
    super(waitHelper);
  }

  @Override
  public void onBeginFailed(Exception ex) {
    log("Session begin failed.", ex);
    performNotify(new AssertionFailedError("Session begin failed: " + ex.getMessage()));
  }

  @Override
  public void onBeginSucceeded(RepositorySession session) {
    log("Session begin succeeded.");
    performNotify();
  }

  @Override
  public RepositorySessionBeginDelegate deferredBeginDelegate(ExecutorService executor) {
    log("Session begin delegate deferred.");
    return this;
  }
}
