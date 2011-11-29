/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.fail;

import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.repositories.domain.Record;

import android.util.Log;

public class ExpectInvalidRequestFetchDelegate extends DefaultRepositorySessionDelegate {
  
  public RepoStatusCode code;
  
  public void fetchCallback(RepoStatusCode status, Record[] records) {
    Log.i("rnewman", "fetchCallback: " + ((status == RepoStatusCode.DONE) ? "DONE" : "NOT DONE"));
    Log.i("rnewman", "fetchCallback: " + ((records == null) ? "null" : "" + records.length) + " records.");

    if (status != null && status.equals(RepoStatusCode.INVALID_REQUEST)) {
      // Track these for test richness.
      this.code = status;
      onDone();
    } else {
      fail("Bad status");
    }
  }
  
  private void onDone() {
    testWaiter().performNotify();
  }
}
