package org.mozilla.android.sync.test.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.repositories.RepositorySessionDelegate;
import org.mozilla.android.sync.repositories.domain.Record;

public class ExpectFetchSinceDelegate extends DefaultRepositorySessionDelegate
    implements RepositorySessionDelegate {
  private String[] expected;
  private long earliest;

  public ExpectFetchSinceDelegate(long timestamp, String[] guids) {
    expected = guids;
    earliest = timestamp;
    Arrays.sort(expected);
  }

  public void fetchSinceCallback(RepoStatusCode status, Record[] records) {
    AssertionError err = null;
    try {
      assertEquals(status, RepoStatusCode.DONE);       // For now.
      assertEquals(records.length, this.expected.length);

      for (Record record : records) {
        assertFalse(-1 == Arrays.binarySearch(this.expected, record.guid));
        assertTrue(record.lastModified >= this.earliest);
      }
    } catch (AssertionError e) {
      err = e;
    }
    testWaiter().performNotify(err);
  }
}
