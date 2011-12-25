/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;

import org.mozilla.gecko.sync.repositories.android.RepoUtils;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.util.Log;

public class DefaultFetchDelegate extends DefaultDelegate implements RepositorySessionFetchRecordsDelegate {

  public ArrayList<Record> records = new ArrayList<Record>();
  
  @Override
  public void onFetchFailed(Exception ex, Record record) {
    sharedFail("Shouldn't fail");
  }

  @Override
  public void onFetchSucceeded(Record[] records, long end) {
    sharedFail("Hit default delegate");
  }

  protected void onDone(ArrayList<Record> records, HashMap<String, Record> expected, long end) {
    Log.i("rnewman", "onDone. Test Waiter is " + testWaiter());
    Log.i("rnewman", "End timestamp is " + end);
    try {
      
      int expectedCount = 0;
      for (String key : expected.keySet()) {
        if (RepoUtils.SPECIAL_GUIDS_MAP == null || !RepoUtils.SPECIAL_GUIDS_MAP.containsKey(key)) {
          expectedCount++;
        }
      }
      for (Record record : records) {
        // Ignore special guids for bookmarks
        if (RepoUtils.SPECIAL_GUIDS_MAP == null || !RepoUtils.SPECIAL_GUIDS_MAP.containsKey(record.guid)) {
          Record expect = expected.get(record.guid);
          if (expect == null) {
            fail("Do not expect to get back a record with guid: " + record.guid);
            testWaiter().performNotify();
          }
          assertEquals(record, expected.get(record.guid));
        }
      }
      assertEquals(expected.size(), expectedCount);
      Log.i("rnewman", "Notifying success.");
      testWaiter().performNotify();
    } catch (AssertionError e) {
      Log.e("rnewman", "Notifying assertion failure.");
      testWaiter().performNotify(e);
    } catch (Exception e) {
      Log.e("rnewman", "Fucking no.");
      testWaiter().performNotify();
    }
  }
  
  public int recordCount() {
    return (this.records == null) ? 0 : this.records.size();
  }

  @Override
  public void onFetchedRecord(Record record) {
    Log.d(LOG_TAG, "onFetchedRecord(" + record.guid + ")");
    records.add(record);
  }

  @Override
  public void onFetchCompleted(long end) {
    Log.d(LOG_TAG, "onFetchCompleted. Doing nothing.");
  }
}
