package org.mozilla.android.sync.test.helpers;

import org.mozilla.android.sync.repositories.InvalidSessionTransitionException;

import static junit.framework.Assert.fail;

public class ExpectBeginFailDelegate extends DefaultBeginDelegate {

  @Override
  public void onBeginFailed(Exception ex) {
    if (ex.getClass() != InvalidSessionTransitionException.class) {
      fail("Wrong exception received");
    }
  }
}