package org.mozilla.android.sync.test;

import org.mozilla.android.sync.repositories.BookmarksRepository;
import org.mozilla.android.sync.repositories.BookmarksRepositorySession;
import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.repositories.RepositoryCallbackReceiver;
import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.SyncCallbackReceiver;
import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.repositories.domain.Record;
import org.mozilla.android.sync.test.CallbackResult.CallType;

import android.content.Context;

public class BookmarksSessionTestWrapper {
  /*
   * This class is basically used to turn async calls into synchronous
   * calls. This is used to make running JUnit tests easier.
   */

  private CallbackResult testResult;

  public CallbackResult doCreateSessionSync(BookmarksRepository repository, Context context, long lastSyncTimestamp) {

    repository.createSession(context, new CallbackReceiver(), lastSyncTimestamp);
    performWait();
    return testResult;
  }

  public CallbackResult doGuidsSinceSync(BookmarksRepositorySession session, long timestamp) {
    session.guidsSince(timestamp, new CallbackReceiver());
    performWait();
    return testResult;
  }

  public CallbackResult doStoreSync(BookmarksRepositorySession session, BookmarkRecord record) {
    session.store(record, new CallbackReceiver());
    performWait();
    return testResult;
  }

  public CallbackResult doFetchAllSync(BookmarksRepositorySession session) {
    session.fetchAll(new CallbackReceiver());
    performWait();
    return testResult;
  }

  public CallbackResult doFetchSync(BookmarksRepositorySession session, String[] guids) {
    session.fetch(guids, new CallbackReceiver());
    performWait();
    return testResult;
  }

  public CallbackResult doFetchSinceSync(BookmarksRepositorySession session, long timestamp) {
    session.fetchSince(timestamp, new CallbackReceiver());
    performWait();
    return testResult;
  }


  class CallbackReceiver implements RepositoryCallbackReceiver, SyncCallbackReceiver {

    public void guidsSinceCallback(RepoStatusCode status, String[] guids) {
      testResult = new CallbackResult(status, CallType.GUIDS_SINCE, guids);
      performNotify();
    }

    public void fetchSinceCallback(RepoStatusCode status, Record[] records) {
      testResult = new CallbackResult(status, CallType.FETCH_SINCE, records);
      performNotify();
    }

    public void fetchCallback(RepoStatusCode status, Record[] records) {
      testResult = new CallbackResult(status, CallType.FETCH, records);
      performNotify();
    }

    public void fetchAllCallback(RepoStatusCode status, Record[] records) {
      testResult = new CallbackResult(status, CallType.FETCH_ALL, records);
      performNotify();
    }

    public void storeCallback(RepoStatusCode status, long rowId) {
      testResult = new CallbackResult(status, CallType.STORE, rowId);
      performNotify();
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

    public void sessionCallback(RepoStatusCode status, RepositorySession session) {
      testResult = new CallbackResult(status, CallType.CREATE_SESSION, session);
      performNotify();
    }

    public void storeCallback(RepoStatusCode status) {
      // TODO Auto-generated method stub

    }
  }

  // Helper to perform the wait
  private synchronized void performWait() {
      try {
        BookmarksSessionTestWrapper.this.wait();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
  }

  // Helper to perform notify
  private synchronized void performNotify() {
    BookmarksSessionTestWrapper.this.notify();
  }

}