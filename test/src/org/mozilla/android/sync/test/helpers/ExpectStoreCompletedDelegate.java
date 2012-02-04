/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import org.mozilla.gecko.sync.repositories.domain.Record;

public class ExpectStoreCompletedDelegate extends DefaultStoreDelegate {

  @Override
  public void onRecordStoreSucceeded(Record record) {
    // That's fine.
  }

  @Override
  public void onStoreCompleted() {
    testWaiter().performNotify();
  }
}