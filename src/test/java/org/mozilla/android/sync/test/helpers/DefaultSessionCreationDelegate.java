package org.mozilla.android.sync.test.helpers;

import static org.junit.Assert.fail;

import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.RepositorySessionCreationDelegate;

public class DefaultSessionCreationDelegate implements RepositorySessionCreationDelegate {
  protected WaitHelper testWaiter() {
    return WaitHelper.getTestWaiter();
  }

  public void onSessionCreateFailed(Exception ex) {
    fail("Should not fail.");
  }

  public void onSessionCreated(RepositorySession session) {
    fail("Should not have been created.");
  }
}