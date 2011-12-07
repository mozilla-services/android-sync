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

  // LEFT OFF HERE, pull this up, write passwords repo and session and tests
  // move on to todo list items that involve others
  @Override
  protected Record reconcileRecords(Record local, Record remote) {
    // TODO Auto-generated method stub
    return null;
  }

}
