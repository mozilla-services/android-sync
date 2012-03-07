/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.mozilla.gecko.sync.repositories.delegates.DeferredRepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

import junit.framework.AssertionFailedError;
import android.util.Log;

public class DefaultFetchDelegate extends DefaultDelegate implements RepositorySessionFetchRecordsDelegate {

  private static final String LOG_TAG = "DefaultFetchDelegate";
  public ArrayList<Record> records = new ArrayList<Record>();
  public Set<String> ignore = new HashSet<String>();

  @Override
  public void onFetchFailed(Exception ex, Record record) {
    performNotify("Fetch failed.", ex);
  }

  @Override
  public void onFetchSucceeded(Record[] records, final long fetchEnd) {
    Log.d(LOG_TAG, "onFetchSucceeded");
    for (Record record : records) {
      this.records.add(record);
    }
    this.onFetchCompleted(fetchEnd);
  }

  protected void onDone(ArrayList<Record> records, HashMap<String, Record> expected, long end) {
    Log.i(LOG_TAG, "onDone.");
    Log.i(LOG_TAG, "End timestamp is " + end);
    Log.i(LOG_TAG, "Expected is " + expected);
    Log.i(LOG_TAG, "Records is " + records);
    try {
      int expectedCount = 0;
      int expectedFound = 0;
      Log.d(LOG_TAG, "Counting expected keys.");
      for (String key : expected.keySet()) {
        if (!ignore.contains(key)) {
          expectedCount++;
        }
      }
      Log.d(LOG_TAG, "Expected keys: " + expectedCount);
      for (Record record : records) {
        Log.d(LOG_TAG, "Record.");
        Log.d(LOG_TAG, record.guid);

        // Ignore special GUIDs (e.g., for bookmarks).
        if (!ignore.contains(record.guid)) {
          Record expect = expected.get(record.guid);
          if (expect == null) {
            fail("Do not expect to get back a record with guid: " + record.guid); // Caught below
          }
          Log.d(LOG_TAG, "Checking equality.");
          try {
            assertTrue(expect.equalPayloads(record)); // Caught below
          } catch (Exception e) {
            Log.e(LOG_TAG, "ONOZ!", e);
          }
          Log.d(LOG_TAG, "Checked equality.");
          expectedFound += 1;
        }
      }
      assertEquals(expectedCount, expectedFound); // Caught below
      Log.i(LOG_TAG, "Notifying success.");
      performNotify();
    } catch (AssertionFailedError e) {
      Log.e(LOG_TAG, "Notifying assertion failure.");
      performNotify(e);
    } catch (Exception e) {
      Log.e(LOG_TAG, "Fucking no.");
      performNotify();
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
  public void onFetchCompleted(final long fetchEnd) {
    Log.d(LOG_TAG, "onFetchCompleted. Doing nothing.");
  }

  @Override
  public RepositorySessionFetchRecordsDelegate deferredFetchDelegate(final ExecutorService executor) {
    return new DeferredRepositorySessionFetchRecordsDelegate(this, executor);
  }
}
