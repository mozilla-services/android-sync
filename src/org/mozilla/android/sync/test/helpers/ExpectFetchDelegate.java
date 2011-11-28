package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.fail;

import java.util.Arrays;

import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.repositories.domain.Record;

import android.util.Log;

public class ExpectFetchDelegate extends DefaultRepositorySessionDelegate {
  public Record[]       records = new Record[0];
  public RepoStatusCode code    = null;
  private String[]      expected;

  public ExpectFetchDelegate(String[] guids) {
    expected = guids;
    Arrays.sort(expected);
  }

  public void fetchCallback(RepoStatusCode status, Record[] records) {
    Log.i("rnewman", "fetchCallback: " + ((status == RepoStatusCode.DONE) ? "DONE" : "NOT DONE"));
    Log.i("rnewman", "fetchCallback: " + ((records == null) ? "null" : "" + records.length) + " records.");

    // Accumulate records.   
    int oldLength = this.records.length;
    this.records = Arrays.copyOf(this.records, oldLength + records.length);
    System.arraycopy(records, 0, this.records, oldLength, records.length);
    if (status != null && status.equals(RepoStatusCode.DONE)) {
      // Track these for test richness.
      this.code = status;
      onDone(this.records, this.expected);
    } else {
      fail("Bad status");
    }
  }

  public int recordCount() {
    return (this.records == null) ? 0 : this.records.length;
  }
  
}