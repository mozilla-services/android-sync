package org.mozilla.android.sync.test.helpers;

import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.RepositorySessionBundle;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFinishDelegate;

public class DefaultFinishDelegate extends DefaultDelegate implements RepositorySessionFinishDelegate {

  @Override
  public void onFinishFailed(Exception ex) {
    sharedFail("Finish failed");
  }

  @Override
  public void onFinishSucceeded(RepositorySession session, RepositorySessionBundle bundle) {
    sharedFail("Hit default finish delegate");
  }
}
