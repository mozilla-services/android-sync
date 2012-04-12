/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.android.ClientsDatabase;
import org.mozilla.gecko.sync.repositories.android.ClientsDatabaseAccessor;
import org.mozilla.gecko.sync.repositories.android.RepoUtils;
import org.mozilla.gecko.sync.repositories.domain.ClientRecord;

import android.database.Cursor;
import android.test.AndroidTestCase;

public class TestClientsDatabase extends AndroidTestCase {

  protected ClientsDatabase db;

  public void setUp() {
    db = new ClientsDatabase(mContext);
    db.wipeDB();
  }

  public void testStoreAndFetch() {
    ClientRecord record = new ClientRecord();
    String profileConst = ClientsDatabaseAccessor.PROFILE_ID;
    db.store(profileConst, record);

    Cursor cur = null;
    try {
      // Test stored item gets fetched correctly.
      cur = db.fetchClientsCursor(record.guid, profileConst);
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
    }
  }

  public void testDelete() {
    ClientRecord record1 = new ClientRecord();
    ClientRecord record2 = new ClientRecord();
    String profileConst = ClientsDatabaseAccessor.PROFILE_ID;

    db.store(profileConst, record1);
    db.store(profileConst, record2);

    Cursor cur = null;
    try {
      // Test record doesn't exist after delete.
      db.deleteClient(record1.guid, profileConst);
      cur = db.fetchClientsCursor(record1.guid, profileConst);
      assertFalse(cur.moveToFirst());
      assertEquals(0, cur.getCount());

      // Test record2 still there after deleting record1.
      cur = db.fetchClientsCursor(record2.guid, profileConst);
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
    }
  }

  public void testWipe() {
    ClientRecord record1 = new ClientRecord();
    ClientRecord record2 = new ClientRecord();
    String profileConst = ClientsDatabaseAccessor.PROFILE_ID;

    db.store(profileConst, record1);
    db.store(profileConst, record2);


    Cursor cur = null;
    try {
      // Test before wipe the records are there.
      cur = db.fetchClientsCursor(record2.guid, profileConst);
      assertTrue(cur.moveToFirst());
      assertEquals(1, cur.getCount());
      cur = db.fetchClientsCursor(record2.guid, profileConst);
      assertTrue(cur.moveToFirst());
      assertEquals(1, cur.getCount());

      // Test after wipe neither record exists.
      db.wipeClientsTable();
      cur = db.fetchClientsCursor(record2.guid, profileConst);
      assertFalse(cur.moveToFirst());
      assertEquals(0, cur.getCount());
      cur = db.fetchClientsCursor(record1.guid, profileConst);
      assertFalse(cur.moveToFirst());
      assertEquals(0, cur.getCount());
    } catch (NullCursorException e) {
      fail("Should not have NullCursorException");
    } finally {
      if (cur != null) {
        cur.close();
      }
    }
  }
}
