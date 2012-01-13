/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;

public class TestRecord {

  @Test
  public void testRecordGUIDs() {
    for (int i = 0; i < 50; ++i) {
      CryptoRecord cryptoRecord = new HistoryRecord().getPayload();
      assertEquals(12, cryptoRecord.guid.length());
      System.out.println(cryptoRecord.toJSONString());
    }
  }

}
