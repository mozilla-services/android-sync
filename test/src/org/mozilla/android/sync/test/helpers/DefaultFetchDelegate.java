/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;

import org.mozilla.gecko.sync.repositories.android.RepoUtils;
import org.mozilla.gecko.sync.repositories.delegates.DeferredRepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.util.Log;

public class DefaultFetchDelegate extends DefaultDelegate implements RepositorySessionFetchRecordsDelegate {

  private static final String LOG_TAG = "DefaultFetchDelegate";
  public ArrayList<Record> records = new ArrayList<Record>();
  
  @Override
  public void onFetchFailed(Exception ex, Record record) {
    sharedFail("Shouldn't fail");
  }

  @Override
  public void onFetchSucceeded(Record[] records, long end) {
    Log.d(LOG_TAG, "onFetchSucceeded");
    for (Record record : records) {
      this.records.add(record);
    }
    this.onFetchCompleted(end);
  }

  protected void onDone(ArrayList<Record> records, HashMap<String, Record> expected, long end) {
    Log.i(LOG_TAG, "onDone. Test Waiter is " + testWaiter());
    Log.i(LOG_TAG, "End timestamp is " + end);
    Log.i(LOG_TAG, "Expected is " + expected);
    Log.i(LOG_TAG, "Records is " + records);
    try {
      int expectedCount = 0;
      Log.d(LOG_TAG, "Counting expected keys.");
      for (String key : expected.keySet()) {
        if (RepoUtils.SPECIAL_GUIDS_MAP == null ||
            !RepoUtils.SPECIAL_GUIDS_MAP.containsKey(key)) {
          expectedCount++;
        }
      }
      Log.d(LOG_TAG, "Expected keys: " + expectedCount);
      for (Record record : records) {
        Log.d(LOG_TAG, "Record.");
        Log.d(LOG_TAG, record.guid);

        // Ignore special guids for bookmarks
        if (RepoUtils.SPECIAL_GUIDS_MAP == null ||
            !RepoUtils.SPECIAL_GUIDS_MAP.containsKey(record.guid)) {
          Record expect = expected.get(record.guid);
          if (expect == null) {
            Log.d(LOG_TAG, "Failing.");
            fail("Do not expect to get back a record with guid: " + record.guid);
            testWaiter().performNotify();
          }
          Log.d(LOG_TAG, "Checking equality.");
          try {
            assertTrue(expect.equalPayloads(record));
          } catch (Exception e) {
            Log.e(LOG_TAG, "ONOZ!", e);
          }
          Log.d(LOG_TAG, "Checked equality.");
        }
      }
      assertEquals(expected.size(), expectedCount);
      Log.i(LOG_TAG, "Notifying success.");
      testWaiter().performNotify();
    } catch (AssertionError e) {
      Log.e(LOG_TAG, "Notifying assertion failure.");
      testWaiter().performNotify(e);
    } catch (Exception e) {
      Log.e(LOG_TAG, "Fucking no.");
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

  @Override
  public RepositorySessionFetchRecordsDelegate deferredFetchDelegate(final ExecutorService executor) {
    return new DeferredRepositorySessionFetchRecordsDelegate(this, executor);
  }
}
