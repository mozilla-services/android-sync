/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.fail;

import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.RepositorySessionCreationDelegate;

public class DefaultSessionCreationDelegate implements RepositorySessionCreationDelegate {
  protected WaitHelper testWaiter() {
    return WaitHelper.getTestWaiter();
  }
  private void sharedFail(String message) {
    try {
      fail(message);
    } catch (AssertionError e) {
      testWaiter().performNotify(e);
    }
  }
  public void onSessionCreateFailed(Exception ex) {
    sharedFail("Should not fail.");
  }

  public void onSessionCreated(RepositorySession session) {
    sharedFail("Should not have been created.");
  }
}
