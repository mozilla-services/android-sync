/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.fail;

import org.mozilla.android.sync.repositories.InvalidRequestException;
import org.mozilla.android.sync.repositories.domain.Record;

import android.util.Log;

public class ExpectInvalidRequestFetchDelegate extends DefaultFetchDelegate {
  
  @Override
  public void onFetchFailed(Exception ex, Record rec) {
    Log.i("rnewman", "ExpectInvalidRequestFetchDelegate got exception " + ex);
    if (ex instanceof InvalidRequestException) {
      onDone();
    } else {
      fail("Wrong exception");
    }
  }
  
  private void onDone() {
    testWaiter().performNotify();
  }
}
