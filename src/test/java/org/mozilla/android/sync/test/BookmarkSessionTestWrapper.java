package org.mozilla.android.sync.test;

import org.mozilla.android.sync.repositories.BookmarksRepositorySession;
import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.repositories.RepositoryCallbackReceiver;
import org.mozilla.android.sync.repositories.domain.Record;

public class BookmarkSessionTestWrapper {

  private GuidsSinceTestResult testResult;

  public GuidsSinceTestResult doGuidsSince(BookmarksRepositorySession session, long timestamp) {
    session.guidsSince(timestamp, new CallbackReceiver());
    synchronized(this) {
      try {
        this.wait(5000);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    return testResult;
  }

  class CallbackReceiver implements RepositoryCallbackReceiver {

    public void guidsSinceCallback(RepoStatusCode status, String[] guids) {
      testResult = new GuidsSinceTestResult(status, guids);
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
