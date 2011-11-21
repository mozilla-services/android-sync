/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

import java.util.Arrays;

import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.repositories.domain.Record;

public class ExpectFetchAllDelegate extends DefaultRepositorySessionDelegate {
  public Record[]       records = new Record[0];
  public RepoStatusCode code    = null;
  private String[]      expected;

  public ExpectFetchAllDelegate(String[] guids) {
    expected = guids;
    Arrays.sort(expected);
  }

  private void onDone() {
    AssertionError err = null;

    try {
      assertEquals(this.expected.length, records.length);

      for (Record record : records) {
        assertFalse(-1 == Arrays.binarySearch(this.expected, record.guid));
      }
    } catch (AssertionError e) {
      err = e;
    }
    testWaiter().performNotify(err);
  }

  public void fetchAllCallback(RepoStatusCode status, Record[] records) {
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
