package org.mozilla.android.sync.test.helpers;

import org.mozilla.android.sync.repositories.delegates.RepositorySessionFinishDelegate;

public class DefaultFinishDelegate extends DefaultDelegate implements RepositorySessionFinishDelegate {

  public void onFinishFailed(Exception ex) {
    sharedFail("Finish failed");
  }

  public void onFinishSucceeded() {
    sharedFail("Hit default finish delegate");
  }

}
