package org.mozilla.android.sync.test.helpers;

import org.mozilla.gecko.sync.repositories.InvalidSessionTransitionException;

public class ExpectFinishFailDelegate extends DefaultFinishDelegate {
  @Override
  public void onFinishFailed(Exception ex) {
    if (!(ex instanceof InvalidSessionTransitionException)) {
      performNotify("Expected InvalidSessionTransititionException but got ", ex);
    }
  }
}
