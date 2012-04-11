/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import java.util.Arrays;
import java.util.HashMap;

import org.mozilla.gecko.sync.repositories.domain.Record;

import android.util.Log;

public class ExpectFetchDelegate extends DefaultFetchDelegate {
  private final static String LOG_TAG = "ExpectFetchDelegate";
  
  private HashMap<String, Record> expect = new HashMap<String, Record>();
  
  public ExpectFetchDelegate(Record[] records) {
    for(int i = 0; i < records.length; i++) {
      expect.put(records[i].guid, records[i]);
    }
  }

  @Override
  public void onFetchSucceeded(Record[] records, final long fetchEnd) {
    Log.i(LOG_TAG, "onFetchSucceeded: " + ((records == null) ? "null" : "" + records.length) + " records.");
    this.records.addAll(Arrays.asList(records));
    this.onFetchCompleted(fetchEnd);
  }

  @Override
  public void onFetchedRecord(Record record) {
    Log.i(LOG_TAG, "onFetchedRecord: " + record.guid);
    this.records.add(record);
  }

  @Override
  public void onFetchCompleted(final long fetchEnd) {
    Log.i(LOG_TAG, "onFetchCompleted: " + fetchEnd);
    Log.i(LOG_TAG, "Records: " + this.recordCount());
    super.onDone(this.records, this.expect, fetchEnd);
  }

  public Record recordAt(int i) {
    return this.records.get(i);
  }
}
