package org.mozilla.android.sync.repositories.android;

import org.mozilla.android.sync.repositories.Repository;
import org.mozilla.android.sync.repositories.domain.Record;

import android.database.Cursor;

public class AndroidBrowserPasswordsRepositorySession extends
    AndroidBrowserRepositorySession {

  public AndroidBrowserPasswordsRepositorySession(Repository repository) {
    super(repository);
    // TODO Auto-generated constructor stub
  }

  @Override
  protected Record recordFromMirrorCursor(Cursor cur) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected String buildRecordString(Record record) {
    // TODO Auto-generated method stub
    return null;
  }

}
