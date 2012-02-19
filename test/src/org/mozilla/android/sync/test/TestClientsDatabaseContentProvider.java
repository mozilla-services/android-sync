package org.mozilla.android.sync.test;

import java.util.ArrayList;

import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.android.ClientsDatabaseContentProvider;
import org.mozilla.gecko.sync.repositories.domain.ClientRecord;

import android.content.Context;
import android.test.AndroidTestCase;

public class TestClientsDatabaseContentProvider extends AndroidTestCase {

  public class StubbedClientsDatabaseContentProvider extends ClientsDatabaseContentProvider {
    private final String accountGUID = Utils.generateGuid();

    public StubbedClientsDatabaseContentProvider(Context mContext) {
      super(mContext, null);
    }

    @Override
    protected String getAccountGUID() {
      return accountGUID;
    }
  }

  public void testStoreArrayListAndFetch() {
    StubbedClientsDatabaseContentProvider db =
        new StubbedClientsDatabaseContentProvider(mContext);
    db.wipe();

    ArrayList<ClientRecord> list = new ArrayList<ClientRecord>();
    ClientRecord record1 = new ClientRecord(Utils.generateGuid());
    ClientRecord record2 = new ClientRecord(Utils.generateGuid());
    ClientRecord record3 = new ClientRecord(Utils.generateGuid());

    list.add(record1);
    list.add(record2);
    db.store(list);

    try {
      ClientRecord r1 = db.fetch(record1.guid);
      ClientRecord r2 = db.fetch(record2.guid);
      ClientRecord r3 = db.fetch(record3.guid);

      assertNotNull(r1);
      assertNotNull(r2);
      assertNull(r3);
      assertTrue(record1.equals(r1));
      assertTrue(record2.equals(r2));
      assertFalse(record3.equals(r3));
    } catch (NullCursorException e) {
      fail("Should not have NullPointerException.");
    }

  }

  public void testNumClients() {
    StubbedClientsDatabaseContentProvider db =
        new StubbedClientsDatabaseContentProvider(mContext);
    db.wipe();

    final int COUNT = 5;
    ArrayList<ClientRecord> list = new ArrayList<ClientRecord>();
    for (int i = 0; i < 5; i++) {
      list.add(new ClientRecord());
    }
    db.store(list);
    assertEquals(COUNT, db.numClients());
  }

  public void testFetchAll() {
    StubbedClientsDatabaseContentProvider db =
        new StubbedClientsDatabaseContentProvider(mContext);
    db.wipe();

    ArrayList<ClientRecord> list = new ArrayList<ClientRecord>();
    ClientRecord record1 = new ClientRecord(Utils.generateGuid());
    ClientRecord record2 = new ClientRecord(Utils.generateGuid());

    list.add(record1);
    list.add(record2);

    try {
      ArrayList<ClientRecord> records =  db.fetchAll();
      assertNotNull(records);
      assertEquals(0, records.size());

      db.store(list);
      records = db.fetchAll();
      assertNotNull(records);
      assertEquals(2, records.size());
      assertTrue(record1.equals(records.get(0)) || record1.equals(records.get(1)));
      assertTrue(record2.equals(records.get(0)) || record2.equals(records.get(1)));
    } catch (NullCursorException e) {
      fail("Should not have NullPointerException.");
    }
  }

  public void testCompareAndStore() {
    StubbedClientsDatabaseContentProvider db =
        new StubbedClientsDatabaseContentProvider(mContext);
    db.wipe();

    try {
      ClientRecord record1 = new ClientRecord();
      ClientRecord record2 = new ClientRecord();
      db.store(record1);

      assertFalse(db.compareAndStore(record1));

      record1.name = "New Name";
      assertTrue(db.compareAndStore(record1));

      assertTrue(db.compareAndStore(record2));
    } catch (NullCursorException e) {
      fail("Should not have NullPointerException.");
    }
  }
}
