package org.mozilla.android.sync.test;

import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.domain.ClientRecord;
import org.mozilla.gecko.sync.repositories.domain.ClientRecordFactory;
import org.mozilla.gecko.sync.setup.Constants;

import android.test.AndroidTestCase;

public class TestClientRecord extends AndroidTestCase{
  public void testEnsureDefaults() {
    // Ensure defaults.
    ClientRecord record = new ClientRecord();
    assertEquals(ClientRecord.COLLECTION_NAME, record.collection);
    assertEquals(0, record.lastModified);
    assertEquals(false, record.deleted);
    assertEquals("Default Name", record.name);
    assertEquals(Constants.CLIENT_TYPE, record.type);
  }

  public void testGetPayload() {
    // Test ClientRecord.getPayload().
    ClientRecord record = new ClientRecord();
    CryptoRecord cryptoRecord = record.getPayload();
    assertEquals(record.guid, cryptoRecord.payload.get("id"));
    assertEquals(null, cryptoRecord.payload.get("collection"));
    assertEquals(null, cryptoRecord.payload.get("lastModified"));
    assertEquals(null, cryptoRecord.payload.get("deleted"));
    assertEquals(record.name, cryptoRecord.payload.get("name"));
    assertEquals(record.type, cryptoRecord.payload.get("type"));
  }

  public void testInitFromPayload() {
    // Test ClientRecord.initFromPayload() in ClientRecordFactory.
    ClientRecord record1 = new ClientRecord();
    CryptoRecord cryptoRecord = record1.getPayload();
    ClientRecordFactory factory = new ClientRecordFactory();
    ClientRecord record2 = (ClientRecord) factory.createRecord(cryptoRecord);
    assertEquals(cryptoRecord.payload.get("id"), record2.guid);
    assertEquals(ClientRecord.COLLECTION_NAME, record2.collection);
    assertEquals(0, record2.lastModified);
    assertEquals(false, record2.deleted);
    assertEquals(cryptoRecord.payload.get("name"), record2.name);
    assertEquals(cryptoRecord.payload.get("type"), record2.type);
  }

  public void testCopyWithIDs() {
    // Test ClientRecord.copyWithIDs.
    ClientRecord record1 = new ClientRecord();
    String newGUID = Utils.generateGuid();
    ClientRecord record2 = (ClientRecord) record1.copyWithIDs(newGUID, 0);
    assertEquals(newGUID, record2.guid);
    assertEquals(0, record2.androidID);
    assertEquals(record1.collection, record2.collection);
    assertEquals(record1.lastModified, record2.lastModified);
    assertEquals(record1.deleted, record2.deleted);
    assertEquals(record1.name, record2.name);
    assertEquals(record1.type, record2.type);
  }

  public void testEquals() {
    // Test ClientRecord.equals().
    ClientRecord record1 = new ClientRecord();
    ClientRecord record2 = new ClientRecord();
    ClientRecord record3 = new ClientRecord(Utils.generateGuid(), "New Name");
    ClientRecord record4 = new ClientRecord(Utils.generateGuid(), Constants.DEFAULT_CLIENT_NAME, "desktop");
    record2.guid = record1.guid;

    assertTrue(record2.equals(record1));
    assertFalse(record3.equals(record1));
    assertFalse(record3.equals(record2));
    assertFalse(record4.equals(record1));
    assertFalse(record4.equals(record2));
  }
}
