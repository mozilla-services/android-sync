/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.fail;

import org.mozilla.gecko.sync.repositories.InvalidSessionTransitionException;

public class ExpectFinishFailDelegate extends DefaultFinishDelegate {
  @Override
  public void onFinishFailed(Exception ex) {
    if (ex.getClass() != InvalidSessionTransitionException.class) {
      fail("Wrong exception received");
    }
  }
}
