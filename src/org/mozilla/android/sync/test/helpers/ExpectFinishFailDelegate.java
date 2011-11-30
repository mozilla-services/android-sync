package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.fail;

import org.mozilla.android.sync.repositories.InvalidSessionTransitionException;

public class ExpectFinishFailDelegate extends DefaultFinishDelegate {
  @Override
  public void onFinishFailed(Exception ex) {
    if (ex.getClass() != InvalidSessionTransitionException.class) {
      fail("Wrong exception received");
    }
  }

}
