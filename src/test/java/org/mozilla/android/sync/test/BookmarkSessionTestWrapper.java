package org.mozilla.android.sync.test;

import org.mozilla.android.sync.repositories.BookmarksRepositorySession;
import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.repositories.RepositoryCallbackReceiver;
import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.repositories.domain.Record;
import org.mozilla.android.sync.test.CallbackResult.CallType;

public class BookmarkSessionTestWrapper {

  private CallbackResult testResult;

  public CallbackResult doGuidsSince(BookmarksRepositorySession session, long timestamp) {
    session.guidsSince(timestamp, new CallbackReceiver());
    synchronized(this) {
      try {
        this.wait(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    return testResult;
  }

  public CallbackResult doStore(BookmarksRepositorySession session, BookmarkRecord record) {
    session.store(record, new CallbackReceiver());

    synchronized(this) {
      try {
        this.wait(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    return testResult;
  }

  public CallbackResult doFetchAll(BookmarksRepositorySession session) {
    session.fetchAll(new CallbackReceiver());

    synchronized(this) {
      try {
        this.wait(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    return testResult;
  }

  class CallbackReceiver implements RepositoryCallbackReceiver {

    public void guidsSinceCallback(RepoStatusCode status, String[] guids) {
      testResult = new CallbackResult(status, CallType.FETCH_SINCE, guids);
      synchronized(BookmarkSessionTestWrapper.this) {
        BookmarkSessionTestWrapper.this.notifyAll();
      }
    }

    public void fetchSinceCallback(RepoStatusCode status, Record[] records) {
      // TODO Auto-generated method stub

    }

    public void fetchCallback(RepoStatusCode status, Record[] records) {
      // TODO Auto-generated method stub

    }

    public void fetchAllCallback(RepoStatusCode status, Record[] records) {
      testResult = new CallbackResult(status, CallType.FETCH_ALL, records);
      synchronized(BookmarkSessionTestWrapper.this) {
        BookmarkSessionTestWrapper.this.notifyAll();
      }

    }

    public void storeCallback(RepoStatusCode status, long rowId) {
      testResult = new CallbackResult(status, CallType.STORE, rowId);
      synchronized(BookmarkSessionTestWrapper.this) {
        BookmarkSessionTestWrapper.this.notifyAll();
      }
    }

    public void wipeCallback(RepoStatusCode status) {
      // TODO Auto-generated method stub

    }

    public void beginCallback(RepoStatusCode status) {
      // TODO Auto-generated method stub

    }

    public void finishCallback(RepoStatusCode status) {
      // TODO Auto-generated method stub

    }
  }
}
