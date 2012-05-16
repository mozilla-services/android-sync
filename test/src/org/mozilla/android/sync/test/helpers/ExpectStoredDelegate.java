/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import junit.framework.AssertionFailedError;

import org.mozilla.gecko.sync.repositories.domain.Record;

public class ExpectStoredDelegate extends DefaultStoreDelegate {
  String expectedGUID;
  String storedGuid;

  public ExpectStoredDelegate(String guid) {
    this.expectedGUID = guid;
  }

  @Override
  public synchronized void onStoreCompleted(long storeEnd) {
    System.out.println("Notifying in onStoreCompleted.");
    try {
      assertNotNull(storedGuid);
      performNotify();
    } catch (AssertionFailedError e) {
      performNotify("GUID " + this.expectedGUID + " was not stored", e);
    }
  }

  @Override
  public synchronized void notifyRecordStoreSucceeded(String guid) {
    this.storedGuid = guid;
    try {
      if (this.expectedGUID != null) {
        assertEquals(this.expectedGUID, guid);
      }
    } catch (AssertionFailedError e) {
      performNotify(e);
    }
  }
}
