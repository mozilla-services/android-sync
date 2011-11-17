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
  private static final int WAIT_TIMEOUT = 500;

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

  // Helper to perform the wait
  private void performWait() {
    synchronized(this) {
      try {
        // TODO: This isn't working properly. For some reason
        // it always goes right up until the timeout, the notify
        // isn't waking up this thread, which would make the tests
        // run quicker if it did!
        this.wait(WAIT_TIMEOUT);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  class CallbackReceiver implements RepositoryCallbackReceiver, SyncCallbackReceiver {

    public void guidsSinceCallback(RepoStatusCode status, String[] guids) {
      testResult = new CallbackResult(status, CallType.GUIDS_SINCE, guids);
      notifyWaitingThreads();
    }

    public void fetchSinceCallback(RepoStatusCode status, Record[] records) {
      testResult = new CallbackResult(status, CallType.FETCH_SINCE, records);
      notifyWaitingThreads();
    }

    public void fetchCallback(RepoStatusCode status, Record[] records) {
      testResult = new CallbackResult(status, CallType.FETCH, records);
      notifyWaitingThreads();
    }

    public void fetchAllCallback(RepoStatusCode status, Record[] records) {
      testResult = new CallbackResult(status, CallType.FETCH_ALL, records);
      notifyWaitingThreads();
    }

    public void storeCallback(RepoStatusCode status, long rowId) {
      testResult = new CallbackResult(status, CallType.STORE, rowId);
      notifyWaitingThreads();
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
      notifyWaitingThreads();
    }

    public void storeCallback(RepoStatusCode status) {
      // TODO Auto-generated method stub

    }

    private void notifyWaitingThreads() {
      synchronized (BookmarksSessionTestWrapper.this) {
        BookmarksSessionTestWrapper.this.notifyAll();
      }
    }
  }
}
