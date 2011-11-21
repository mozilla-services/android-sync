/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static junit.framework.Assert.assertEquals;

import org.mozilla.android.sync.repositories.domain.Record;

public class ExpectStoredDelegate extends DefaultStoreDelegate {
  String expectedGUID;

  public ExpectStoredDelegate(String guid) {
    this.expectedGUID = guid;
  }

  public ExpectStoredDelegate() {
  }

  public void onStoreSucceeded(Record record) {
    try {
      if (this.expectedGUID != null) {
        assertEquals(this.expectedGUID, record.guid);
      }
      testWaiter().performNotify();
    } catch (AssertionError e) {
      testWaiter().performNotify(e);
    }
  }
}
