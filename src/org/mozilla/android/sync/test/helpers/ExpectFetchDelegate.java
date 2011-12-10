/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import java.util.Arrays;

import org.mozilla.android.sync.repositories.domain.Record;

import android.util.Log;

public class ExpectFetchDelegate extends DefaultFetchDelegate {
  private String[] expected;

  public ExpectFetchDelegate(String[] guids) {
    expected = guids;
    Arrays.sort(expected);
  }

  @Override
  public void onFetchSucceeded(Record[] records, long end) {
    Log.i("rnewman", "fetchCallback: " + ((records == null) ? "null" : "" + records.length) + " records.");
    this.records.addAll(Arrays.asList(records));
    this.onFetchCompleted(end);
  }

  @Override
  public void onFetchedRecord(Record record) {
    this.records.add(record);
  }

  @Override
  public void onFetchCompleted(long end) {
    super.onDone(this.records, this.expected, end);
  }

  public Record recordAt(int i) {
    return this.records.get(i);
  }
}