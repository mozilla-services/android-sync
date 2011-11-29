/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.fail;

import org.mozilla.android.sync.repositories.InvalidRequestException;

public class ExpectInvalidRequestFetchDelegate extends DefaultFetchDelegate {
  
  public void onFetchFailed(Exception ex) {
    if (ex.getClass() == InvalidRequestException.class) {
      onDone();
    } else {
      fail("Wrong exception");
    }
  }
  
  private void onDone() {
    testWaiter().performNotify();
  }
}
