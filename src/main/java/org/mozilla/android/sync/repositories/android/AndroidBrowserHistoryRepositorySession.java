package org.mozilla.android.sync.repositories.android;

import org.mozilla.android.sync.repositories.Repository;
import org.mozilla.android.sync.repositories.domain.Record;

import android.content.Context;
import android.database.Cursor;

public class AndroidBrowserHistoryRepositorySession extends AndroidBrowserRepositorySession {

  public AndroidBrowserHistoryRepositorySession(Repository repository,
      Context context, long lastSyncTimestamp) {
    super(repository, lastSyncTimestamp);
    dbHelper = new AndroidBrowserHistoryDatabaseHelper(context);
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
