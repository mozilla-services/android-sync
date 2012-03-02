/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.assertEquals;

import org.mozilla.gecko.sync.repositories.domain.Record;

import junit.framework.AssertionFailedError;

public class ExpectStoredDelegate extends DefaultStoreDelegate {
  String expectedGUID;
  Record storedRecord;

  public ExpectStoredDelegate(String guid) {
    this.expectedGUID = guid;
  }

  @Override
  public synchronized void onStoreCompleted(long storeEnd) {
    if (this.storedRecord == null) {
      System.out.println("Notifying in onStoreCompleted.");
      performNotify();
    }
  }

  @Override
  public synchronized void onRecordStoreSucceeded(Record record) {
    this.storedRecord = record;
    try {
      if (this.expectedGUID != null) {
        assertEquals(this.expectedGUID, record.guid);
      }
      System.out.println("Notifying in onRecordStoreSucceeded.");
      performNotify();
    } catch (AssertionFailedError e) {
      performNotify(e);
    }
  }
}
