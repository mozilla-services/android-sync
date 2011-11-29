/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import java.util.Arrays;

import org.mozilla.android.sync.repositories.domain.Record;

import android.util.Log;

public class ExpectFetchAllDelegate extends DefaultFetchDelegate {
  private String[]      expected;

  public ExpectFetchAllDelegate(String[] guids) {
    expected = guids;
    Arrays.sort(expected);
  }

  @Override
  public void onFetchSucceeded(Record[] records) {
    Log.i("rnewman", "fetchAllCallback: " + ((records == null) ? "null" : "" + records.length) + " records.");

    // Accumulate records.   
    int oldLength = this.records.length;
    this.records = Arrays.copyOf(this.records, oldLength + records.length);
    System.arraycopy(records, 0, this.records, oldLength, records.length);
    onDone(this.records, this.expected);
  }

}
