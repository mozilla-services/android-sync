/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import java.util.Arrays;
import java.util.HashMap;

import org.mozilla.gecko.sync.repositories.domain.Record;

import android.util.Log;

public class ExpectFetchDelegate extends DefaultFetchDelegate {
  
  private HashMap<String, Record> expect = new HashMap<String, Record>();
  
  public ExpectFetchDelegate(Record[] records) {
    for(int i = 0; i < records.length; i++) {
      expect.put(records[i].guid, records[i]);
    }
  }

  @Override
  public void onFetchSucceeded(Record[] records, long end) {
    Log.i("ExpectFetchDelegate", "onFetchSucceeded: " + ((records == null) ? "null" : "" + records.length) + " records.");
    this.records.addAll(Arrays.asList(records));
    this.onFetchCompleted(end);
  }

  @Override
  public void onFetchedRecord(Record record) {
    Log.i("ExpectFetchDelegate", "onFetchedRecord: " + record.guid);
    this.records.add(record);
  }

  @Override
  public void onFetchCompleted(long end) {
    Log.i("ExpectFetchDelegate", "onFetchCompleted: " + end);
    Log.i("ExpectFetchDelegate", "Records: " + this.recordCount());
    super.onDone(this.records, this.expect, end);
  }

  public Record recordAt(int i) {
    return this.records.get(i);
  }
}
