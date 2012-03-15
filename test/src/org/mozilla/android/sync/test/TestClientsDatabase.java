/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.android.ClientsDatabase;
import org.mozilla.gecko.sync.repositories.android.ClientsDatabaseAccessor;
import org.mozilla.gecko.sync.repositories.android.RepoUtils;
import org.mozilla.gecko.sync.repositories.domain.ClientRecord;

import android.database.Cursor;
import android.test.AndroidTestCase;

public class TestClientsDatabase extends AndroidTestCase {
  public void testStoreAndFetch() {
    ClientsDatabase db = new ClientsDatabase(mContext);
    db.wipe();
    ClientRecord record = new ClientRecord();
    String profileConst = ClientsDatabaseAccessor.PROFILE_ID;
    db.store(profileConst, record);

    Cursor cur = null;
    try {
      // Test stored item gets fetched correctly.
      cur = db.fetch(record.guid, profileConst);
      assertTrue(cur.moveToFirst());
      assertEquals(1, cur.getCount());

      String guid = RepoUtils.getStringFromCursor(cur, ClientsDatabase.COL_ACCOUNT_GUID);
      String profileId = RepoUtils.getStringFromCursor(cur, ClientsDatabase.COL_PROFILE);
      String clientName = RepoUtils.getStringFromCursor(cur, ClientsDatabase.COL_NAME);
      String clientType = RepoUtils.getStringFromCursor(cur, ClientsDatabase.COL_TYPE);

      assertEquals(record.guid, guid);
      assertEquals(profileConst, profileId);
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
    String profileConst = ClientsDatabaseAccessor.PROFILE_ID;

    db.store(profileConst, record1);
    db.store(profileConst, record2);

    Cursor cur = null;
    try {
      // Test record doesn't exist after delete.
      db.delete(record1.guid, profileConst);
      cur = db.fetch(record1.guid, profileConst);
      assertFalse(cur.moveToFirst());
      assertEquals(0, cur.getCount());

      // Test record2 still there after deleting record1.
      cur = db.fetch(record2.guid, profileConst);
      assertTrue(cur.moveToFirst());
      assertEquals(1, cur.getCount());

      String guid = RepoUtils.getStringFromCursor(cur, ClientsDatabase.COL_ACCOUNT_GUID);
      String profileId = RepoUtils.getStringFromCursor(cur, ClientsDatabase.COL_PROFILE);
      String clientName = RepoUtils.getStringFromCursor(cur, ClientsDatabase.COL_NAME);
      String clientType = RepoUtils.getStringFromCursor(cur, ClientsDatabase.COL_TYPE);

      assertEquals(record2.guid, guid);
      assertEquals(profileConst, profileId);
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
    String profileConst = ClientsDatabaseAccessor.PROFILE_ID;

    db.store(profileConst, record1);
    db.store(profileConst, record2);


    Cursor cur = null;
    try {
      // Test before wipe the records are there.
      cur = db.fetch(record2.guid, profileConst);
      assertTrue(cur.moveToFirst());
      assertEquals(1, cur.getCount());
      cur = db.fetch(record2.guid, profileConst);
      assertTrue(cur.moveToFirst());
      assertEquals(1, cur.getCount());

      // Test after wipe neither record exists.
      db.wipe();
      cur = db.fetch(record2.guid, profileConst);
      assertFalse(cur.moveToFirst());
      assertEquals(0, cur.getCount());
      cur = db.fetch(record1.guid, profileConst);
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
