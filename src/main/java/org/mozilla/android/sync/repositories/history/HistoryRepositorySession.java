package org.mozilla.android.sync.repositories.history;

import org.mozilla.android.sync.repositories.Repository;
import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.android.sync.repositories.domain.Record;

import android.content.Context;
import android.database.Cursor;

public class HistoryRepositorySession extends RepositorySession {

  public HistoryRepositorySession(Repository repository,
      RepositorySessionCreationDelegate delegate, Context context, long lastSyncTimestamp) {
    super(repository, delegate, lastSyncTimestamp);
    dbHelper = new HistoryDatabaseHelper(context);
  }

  @Override
  protected Record[] compileIntoRecordsArray(Cursor cur) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected Record reconcileRecords(Record local, Record remote) {
    // TODO Auto-generated method stub
    return null;
  }

}
