package org.mozilla.android.sync.test.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;

import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.repositories.domain.Record;

public class ExpectFetchAllDelegate extends DefaultRepositorySessionDelegate {
  public Record[]       records = null;
  public RepoStatusCode code    = null;
  private String[]      expected;

  public ExpectFetchAllDelegate(String[] guids) {
    expected = guids;
    Arrays.sort(expected);
  }

  public void fetchAllCallback(RepoStatusCode status, Record[] records) {
    AssertionError err = null;
    try {
      // We're assuming that we get called in one batch. That won't always be
      // the case.
      assertEquals(status, RepoStatusCode.DONE);
      assertEquals(records.length, this.expected.length);

      // Track these for test richness.
      this.records = records;
      this.code = status;
      for (Record record : records) {
        assertFalse(-1 == Arrays.binarySearch(this.expected, record.guid));
      }
    } catch (AssertionError e) {
      err = e;
    }
    testWaiter().performNotify(err);
  }
}