/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories.android;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.mozilla.gecko.sync.CommandProcessor.Command;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.domain.ClientRecord;

import android.content.Context;
import android.database.Cursor;

public class ClientsDatabaseAccessor {

  public static final String PROFILE_ID = "default";     // Generic profile id for now, until multiple profiles are implemented.
  public static final String LOG_TAG = "ClientsDatabaseAccessor";

  private ClientsDatabase db;

  // Need this so we can properly stub out the class for testing.
  public ClientsDatabaseAccessor() {}

  public ClientsDatabaseAccessor(Context context) {
    db = new ClientsDatabase(context);
  }

  public void store(ClientRecord record) {
    db.store(getProfileId(), record);
  }

  public void store(Collection<ClientRecord> records) {
    for (ClientRecord record : records) {
      this.store(record);
    }
  }

  public void store(String accountGUID, Command command) throws NullCursorException {
    db.store(accountGUID, command.commandType, command.args.toJSONString());
  }

  public ClientRecord fetchClient(String accountGUID) throws NullCursorException {
    Cursor cur = null;
    try {
      cur = db.fetchClientsCursor(accountGUID, getProfileId());

      if (cur == null || !cur.moveToFirst()) {
        return null;
      }
      return recordFromCursor(cur);
    } finally {
      if (cur != null) {
        cur.close();
      }
    }
  }

  public Map<String, ClientRecord> fetchAllClients() throws NullCursorException {
    HashMap<String, ClientRecord> map = new HashMap<String, ClientRecord>();
    Cursor cur = null;
    try {
      cur = db.fetchAllClients();
      if (cur == null || !cur.moveToFirst()) {
        return Collections.unmodifiableMap(map);
      }
      while (!cur.isAfterLast()) {
        ClientRecord clientRecord = recordFromCursor(cur);
        map.put(clientRecord.guid, clientRecord);
        cur.moveToNext();
      }

      return Collections.unmodifiableMap(map);
    } finally {
      if (cur != null) {
        cur.close();
      }
    }
  }

  public List<Command> fetchAllCommands() throws NullCursorException {
    List<Command> commands = new ArrayList<Command>();
    Cursor cur = null;
    try {
      cur = db.fetchAllCommands();
      if (cur == null || !cur.moveToFirst()) {
        return Collections.unmodifiableList(commands);
      }
      while(!cur.isAfterLast()) {
        Command command = commandFromCursor(cur);
        commands.add(command);
        cur.moveToNext();
      }

      return Collections.unmodifiableList(commands);
    } finally {
      if (cur != null) {
        cur.close();
      }
    }
  }

  public List<Command> fetchCommandsForClient(String accountGUID) throws NullCursorException {
    List<Command> commands = new ArrayList<Command>();
    Cursor cur = null;
    try {
      cur = db.fetchCommandsForClient(accountGUID);
      if (cur == null || !cur.moveToFirst()) {
        return Collections.unmodifiableList(commands);
      }
      while(!cur.isAfterLast()) {
        Command command = commandFromCursor(cur);
        commands.add(command);
        cur.moveToNext();
      }

      return Collections.unmodifiableList(commands);
    } finally {
      if (cur != null) {
        cur.close();
      }
    }
  }

  protected ClientRecord recordFromCursor(Cursor cur) {
    String accountGUID = RepoUtils.getStringFromCursor(cur, ClientsDatabase.COL_ACCOUNT_GUID);
    String clientName = RepoUtils.getStringFromCursor(cur, ClientsDatabase.COL_NAME);
    String clientType = RepoUtils.getStringFromCursor(cur, ClientsDatabase.COL_TYPE);
    ClientRecord record = new ClientRecord(accountGUID);
    record.name = clientName;
    record.type = clientType;
    return record;
  }

  protected Command commandFromCursor(Cursor cur) {
    String commandType = RepoUtils.getStringFromCursor(cur, ClientsDatabase.COL_COMMAND);
    JSONArray commandArgs = RepoUtils.getJSONArrayFromCursor(cur, ClientsDatabase.COL_ARGS);
    return new Command(commandType, commandArgs);
  }

  public int clientsCount() {
    Cursor cur;
    try {
      cur = db.fetchAllClients();
    } catch (NullCursorException e) {
      return 0;
    }
    try {
      return cur.getCount();
    } finally {
      cur.close();
    }
  }

  private String getProfileId() {
    return ClientsDatabaseAccessor.PROFILE_ID;
  }

  public void wipeDB() {
    db.wipeDB();
  }

  public void wipeClientsTable() {
    db.wipeClientsTable();
  }

  public void wipeCommandsTable() {
    db.wipeCommandsTable();
  }

  public void close() {
    db.close();
  }
}
