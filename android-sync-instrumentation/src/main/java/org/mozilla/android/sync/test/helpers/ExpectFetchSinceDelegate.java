/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.util.Arrays;
import java.util.Map;

import org.mozilla.gecko.sync.repositories.android.RepoUtils;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.util.Log;

public class ExpectFetchSinceDelegate extends DefaultFetchDelegate {
  private String[] expected;
  private long earliest;

  public ExpectFetchSinceDelegate(long timestamp, String[] guids) {
    expected = guids;
    earliest = timestamp;
    Arrays.sort(expected);
  }

  @Override
  public void onFetchCompleted(long end) {
    Log.i("ExpectFetchSinceDelegate", "onFetchCompleted.");
    AssertionError err = null;
    try {

      Map<String, String> specialGuids = RepoUtils.SPECIAL_GUIDS_MAP;
      int countSpecials = 0;
      for (Record record : records) {
        if (specialGuids == null || !specialGuids.containsKey(record.guid)) {
          assertFalse(-1 == Arrays.binarySearch(this.expected, record.guid));
        } else {
          countSpecials++;
        }
        assertTrue(record.lastModified >= this.earliest);
      }
      assertEquals(this.expected.length, records.size() - countSpecials);
    } catch (AssertionError e) {
      err = e;
    }
    testWaiter().performNotify(err);
  }
}
