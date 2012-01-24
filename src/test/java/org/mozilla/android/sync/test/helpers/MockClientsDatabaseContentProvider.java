package org.mozilla.android.sync.test.helpers;

import java.util.ArrayList;

import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.android.ClientsDatabaseContentProvider;
import org.mozilla.gecko.sync.repositories.domain.ClientRecord;

public class MockClientsDatabaseContentProvider extends ClientsDatabaseContentProvider {
  @Override
  public void store(ClientRecord record) {}

  @Override
  public void store(ArrayList<ClientRecord> records) {}

  @Override
  public boolean compareAndStore(ClientRecord newRecord) throws NullCursorException {
    return true;
  }

  @Override
  public ClientRecord fetch(String profileID) throws NullCursorException {
    return null;
  }

  @Override
  public ArrayList<ClientRecord> fetchAll() throws NullCursorException {
    return null;
  }

  @Override
  public int numClients() {
    return 0;
  }

  @Override
  public void wipe() {}

  @Override
  public void close() {}
}