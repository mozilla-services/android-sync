package org.mozilla.android.sync.test.helpers;

import java.util.Collection;
import java.util.Map;

import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.android.ClientsDatabaseAccessor;
import org.mozilla.gecko.sync.repositories.domain.ClientRecord;

public class MockClientsDatabaseAccessor extends ClientsDatabaseAccessor {
  public boolean storedRecord = false;
  public boolean wiped = false;
  public boolean closed = false;
  public boolean storedArrayList = false;

  @Override
  public void store(ClientRecord record) {
    storedRecord = true;
  }

  @Override
  public void store(Collection<ClientRecord> records) {
    storedArrayList = false;
  }

  @Override
  public ClientRecord fetch(String profileID) throws NullCursorException {
    return null;
  }

  @Override
  public Map<String, ClientRecord> fetchAll() throws NullCursorException {
    return null;
  }

  @Override
  public int clientsCount() {
    return 0;
  }

  @Override
  public void wipe() {
    wiped = true;
  }

  @Override
  public void close() {
    closed = true;
  }

  public void resetVars() {
    storedRecord = wiped = closed = storedArrayList = false;
  }
}