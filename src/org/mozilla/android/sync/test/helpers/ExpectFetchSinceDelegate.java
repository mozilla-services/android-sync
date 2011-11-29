/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.util.Arrays;

import org.mozilla.android.sync.repositories.domain.Record;

public class ExpectFetchSinceDelegate extends DefaultFetchSinceDelegate {
  private String[] expected;
  private long earliest;

  public ExpectFetchSinceDelegate(long timestamp, String[] guids) {
    expected = guids;
    earliest = timestamp;
    Arrays.sort(expected);
  }

  public void onFetchSinceSucceeded(Record[] records) {
    AssertionError err = null;
    try {
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
