/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

import java.util.Arrays;

import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.repositories.domain.Record;

import android.util.Log;

public class ExpectFetchAllDelegate extends DefaultRepositorySessionDelegate {
  public Record[]       records = new Record[0];
  public RepoStatusCode code    = null;
  private String[]      expected;

  public ExpectFetchAllDelegate(String[] guids) {
    expected = guids;
    Arrays.sort(expected);
  }

  private void onDone() {
    Log.i("rnewman", "onDone. Test Waiter is " + testWaiter());
    try {
      assertEquals(this.expected.length, records.length);
      for (Record record : records) {
        assertFalse(-1 == Arrays.binarySearch(this.expected, record.guid));
      }
      Log.i("rnewman", "Notifying success.");
      testWaiter().performNotify();
    } catch (AssertionError e) {
      Log.i("rnewman", "Notifying assertion failure.");
      testWaiter().performNotify(e);
    } catch (Exception e) {
      Log.i("rnewman", "Fucking no.");
      testWaiter().performNotify();
    }
  }

  public void fetchAllCallback(RepoStatusCode status, Record[] records) {
    Log.i("rnewman", "fetchAllCallback: " + ((status == RepoStatusCode.DONE) ? "DONE" : "NOT DONE"));
    Log.i("rnewman", "fetchAllCallback: " + ((records == null) ? "null" : "" + records.length) + " records.");

    // Accumulate records.   
    int oldLength = this.records.length;
    this.records = Arrays.copyOf(this.records, oldLength + records.length);
    System.arraycopy(records, 0, this.records, oldLength, records.length);
    if (status != null && status.equals(RepoStatusCode.DONE)) {
      // Track these for test richness.
      this.code = status;
      this.onDone();
    }
  }

  public int recordCount() {
    // TODO Auto-generated method stub
    return (this.records == null) ? 0 : this.records.length;
  }
}
