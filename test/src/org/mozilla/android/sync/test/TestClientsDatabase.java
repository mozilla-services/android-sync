package org.mozilla.android.sync.test;

import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.android.ClientsDatabase;
import org.mozilla.gecko.sync.repositories.android.RepoUtils;
import org.mozilla.gecko.sync.repositories.domain.ClientRecord;
import android.database.Cursor;
import android.test.AndroidTestCase;

public class TestClientsDatabase extends AndroidTestCase {
  public void testStoreAndFetch() {
    ClientsDatabase db = new ClientsDatabase(mContext);
    db.wipe();
    ClientRecord record = new ClientRecord();
    String accountGUID = Utils.generateGuid();
    db.store(accountGUID, record);

    Cursor cur = null;
    try {
      // Test stored item gets fetched correctly.
      cur = db.fetch(accountGUID, record.guid);
      assertTrue(cur.moveToFirst());
      assertEquals(1, cur.getCount());

      String guid = RepoUtils.getStringFromCursor(cur, ClientsDatabase.COL_ACCOUNT_GUID);
      String profileId = RepoUtils.getStringFromCursor(cur, ClientsDatabase.COL_PROFILE);
      String clientName = RepoUtils.getStringFromCursor(cur, ClientsDatabase.COL_NAME);
      String clientType = RepoUtils.getStringFromCursor(cur, ClientsDatabase.COL_TYPE);

      assertEquals(accountGUID, guid);
      assertEquals(record.guid, profileId);
      assertEquals(record.name, clientName);
      assertEquals(record.type, clientType);
    } catch (NullCursorException e) {
      fail("Should not have NullCursorException");
    } finally {
      if (cur != null) {
        cur.close();
      }
      db.close();
    }
  }

  public void testDelete() {
    ClientsDatabase db = new ClientsDatabase(mContext);
    db.wipe();
    ClientRecord record1 = new ClientRecord();
    ClientRecord record2 = new ClientRecord();
    String accountGUID = Utils.generateGuid();

    db.store(accountGUID, record1);
    db.store(accountGUID, record2);

    // Test record doesn't exist after delete.
    Cursor cur = null;
    try {
      db.delete(accountGUID, record1.guid);
      cur = db.fetch(accountGUID, record1.guid);
      assertFalse(cur.moveToFirst());
      assertEquals(0, cur.getCount());

      // Test record2 still there after deleting record1.
      cur = db.fetch(accountGUID, record2.guid);
      assertTrue(cur.moveToFirst());
      assertEquals(1, cur.getCount());

      String guid = RepoUtils.getStringFromCursor(cur, ClientsDatabase.COL_ACCOUNT_GUID);
      String profileId = RepoUtils.getStringFromCursor(cur, ClientsDatabase.COL_PROFILE);
      String clientName = RepoUtils.getStringFromCursor(cur, ClientsDatabase.COL_NAME);
      String clientType = RepoUtils.getStringFromCursor(cur, ClientsDatabase.COL_TYPE);

      assertEquals(accountGUID, guid);
      assertEquals(record2.guid, profileId);
      assertEquals(record2.name, clientName);
      assertEquals(record2.type, clientType);
    } catch (NullCursorException e) {
      fail("Should not have NullCursorException");
    } finally {
      if (cur != null) {
        cur.close();
      }
      db.close();
    }
  }

  public void testWipe() {
    ClientsDatabase db = new ClientsDatabase(mContext);
    db.wipe();
    ClientRecord record1 = new ClientRecord();
    ClientRecord record2 = new ClientRecord();
    String accountGUID = Utils.generateGuid();

    db.store(accountGUID, record1);
    db.store(accountGUID, record2);


    Cursor cur = null;
    try {
      // Test before wipe the records are there.
      cur = db.fetch(accountGUID, record2.guid);
      assertTrue(cur.moveToFirst());
      assertEquals(1, cur.getCount());
      cur = db.fetch(accountGUID, record2.guid);
      assertTrue(cur.moveToFirst());
      assertEquals(1, cur.getCount());

      // Test after wipe neither record exists.
      db.wipe();
      cur = db.fetch(accountGUID, record2.guid);
      assertFalse(cur.moveToFirst());
      assertEquals(0, cur.getCount());
      cur = db.fetch(accountGUID, record1.guid);
      assertFalse(cur.moveToFirst());
      assertEquals(0, cur.getCount());
    } catch (NullCursorException e) {
      fail("Should not have NullCursorException");
    } finally {
      if (cur != null) {
        cur.close();
      }
      db.close();
    }
  }
}
