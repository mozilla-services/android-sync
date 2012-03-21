/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.assertEquals;
import junit.framework.AssertionFailedError;

import org.mozilla.gecko.sync.repositories.domain.Record;

import android.util.Log;

public class ExpectStoredDelegate extends DefaultStoreDelegate {
  String expectedGUID;
  Record storedRecord;

  public ExpectStoredDelegate(String guid) {
    this.expectedGUID = guid;
  }

  @Override
  public synchronized void onStoreCompleted(long storeEnd) {
    performNotify();
  }

  @Override
  public synchronized void onRecordStoreSucceeded(Record record) {
    Log.d("ExpectStoredDelegate", "RecordStoreSucceeded");
    this.storedRecord = record;
    try {
      if (this.expectedGUID != null) {
        assertEquals(this.expectedGUID, record.guid);
      }
    } catch (AssertionFailedError e) {
      performNotify(e);
    }
  }
}
