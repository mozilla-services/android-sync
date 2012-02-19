package org.mozilla.gecko.sync.repositories.android;

import java.util.ArrayList;

import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.domain.ClientRecord;

import android.content.Context;
import android.database.Cursor;

public class ClientsDatabaseContentProvider {

  public static final String LOG_TAG = "ClientsDatabaseContentProvider";

  private GlobalSession session;
  private ClientsDatabase db;

  // Need this so we can properly stub out the class for testing.
  public ClientsDatabaseContentProvider() {}

  public ClientsDatabaseContentProvider(Context context, GlobalSession session) {
    this.session = session;
    db = new ClientsDatabase(context);
  }

  public void store(ClientRecord record) {
    db.store(getAccountGUID(), record);
  }

  public void store(ArrayList<ClientRecord> records) {
    for (ClientRecord record : records) {
      this.store(record);
    }
  }

  /**
   * If no record exists with the same id as newRecord or if it exists but
   * is not equal, newRecord will be inserted into the database. If newRecord
   * was successfully inserted, return true. Otherwise return false.
   *
   * @param newRecord
   * @return
   * @throws NullCursorException
   */
  public boolean compareAndStore(ClientRecord newRecord) throws NullCursorException {
    ClientRecord oldRecord = this.fetch(newRecord.guid);

    if (oldRecord == null || !oldRecord.equals(newRecord)) {
      db.store(getAccountGUID(), newRecord);
      return true;
    }
    return false;
  }

  public ClientRecord fetch(String profileID) throws NullCursorException {
    Cursor cur = null;
    try {
      cur = db.fetch(getAccountGUID(), profileID);

      if (!cur.moveToFirst()) {
        return null;
      }
      return recordFromCursor(cur);
    } finally {
      if (cur != null) {
        cur.close();
      }
    }
  }

  public ArrayList<ClientRecord> fetchAll() throws NullCursorException {
    ArrayList<ClientRecord> list = new ArrayList<ClientRecord>();
    Cursor cur = null;
    try {
      cur = db.fetchAll();
      if (!cur.moveToFirst()) {
        return list;
      }
      while (!cur.isAfterLast()) {
        list.add(recordFromCursor(cur));
        cur.moveToNext();
      }
      return list;
    } finally {
      if (cur != null) {
        cur.close();
      }
    }
  }

  protected ClientRecord recordFromCursor(Cursor cur) {
    String profileID = RepoUtils.getStringFromCursor(cur, ClientsDatabase.COL_PROFILE);
    String clientName = RepoUtils.getStringFromCursor(cur, ClientsDatabase.COL_NAME);
    String clientType = RepoUtils.getStringFromCursor(cur, ClientsDatabase.COL_TYPE);
    ClientRecord record = new ClientRecord(profileID);
    record.name = clientName;
    record.type = clientType;
    return record;
  }

  protected String getAccountGUID() {
    return session.getAccountGUID();
  }

  public int numClients() {
    try {
      return db.fetchAll().getCount();
    } catch (NullCursorException e) {
      return 0;
    }
  }

  public void wipe() {
    db.wipe();
  }

  public void close() {
    db.close();
  }
}
