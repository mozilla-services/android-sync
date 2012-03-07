

package org.mozilla.gecko.sync.repositories.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.domain.FormHistoryRecord;

public class TestFormHistoryRecord {

  @Test
  public void testCollection() {
    FormHistoryRecord fr = new FormHistoryRecord();
    assertEquals("formhistory", fr.collection);
  }

  @Test
  public void testGetPayload() {
    FormHistoryRecord fr = FormHistoryRecord.withIdFieldNameAndValue(0, "username", "aUsername");
    CryptoRecord rec = fr.getEnvelope();
    assertEquals("username",  rec.payload.get("fieldName"));
    assertEquals("aUsername", rec.payload.get("value"));
  }

  @Test
  public void testCopyWithIDs() {
    FormHistoryRecord fr = FormHistoryRecord.withIdFieldNameAndValue(0, "username", "aUsername");
    String guid = Utils.generateGuid();
    FormHistoryRecord fr2 = (FormHistoryRecord)fr.copyWithIDs(guid, 9999);
    assertEquals(guid, fr2.guid);
    assertEquals(9999, fr2.androidID);
    assertEquals(fr.fieldName, fr2.fieldName);
    assertEquals(fr.fieldValue, fr2.fieldValue);
  }

  @Test
  public void testEquals() {
    FormHistoryRecord fr1a = FormHistoryRecord.withIdFieldNameAndValue(0, "username1", "Alice");
    FormHistoryRecord fr1b = FormHistoryRecord.withIdFieldNameAndValue(0, "username1", "Bob");
    FormHistoryRecord fr2a = FormHistoryRecord.withIdFieldNameAndValue(0, "username2", "Alice");
    FormHistoryRecord fr2b = FormHistoryRecord.withIdFieldNameAndValue(0, "username2", "Bob");

    assertFalse(fr1a.equals(fr1b));
    assertFalse(fr1a.equals(fr2a));
    assertFalse(fr1a.equals(fr2b));
    assertFalse(fr1b.equals(fr2a));
    assertFalse(fr1b.equals(fr2b));
    assertFalse(fr2a.equals(fr2b));

    assertFalse(fr1a.equals(FormHistoryRecord.withIdFieldNameAndValue(fr1a.androidID, fr1a.fieldName, fr1b.fieldValue)));
    assertFalse(fr1a.equals(fr1a.copyWithIDs(fr2a.guid, 9999)));
    assertTrue(fr1a.equals(fr1a));
  }
}
