package org.mozilla.android.sync.test;

import static junit.framework.Assert.assertNotNull;
import junit.framework.TestSuite;

import org.mozilla.android.sync.MainActivity;
import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.bookmarks.BookmarksRepository;
import org.mozilla.android.sync.repositories.bookmarks.BookmarksRepositorySession;
import org.mozilla.android.sync.test.helpers.DefaultSessionCreationDelegate;
import org.mozilla.android.sync.test.helpers.ExpectNoGUIDsSinceDelegate;
import org.mozilla.android.sync.test.helpers.WaitHelper;

import android.content.Context;

public class AndroidBookmarksTestHelper extends TestSuite {

  public static WaitHelper testWaiter = WaitHelper.getTestWaiter();
  public static BookmarksRepositorySession session;

  public class SetupDelegate extends DefaultSessionCreationDelegate {
    public void onSessionCreated(RepositorySession sess) {
      AssertionError err = null;
      try {
        assertNotNull(sess);
        session = (BookmarksRepositorySession) sess;
      } catch (AssertionError e) {
        err = e;
      }
      testWaiter.performNotify(err);
    }
  }

  public AndroidBookmarksTestHelper() {
    super();
  }

  public BookmarksRepository prepareRepositorySession(DefaultSessionCreationDelegate delegate, long lastSyncTimestamp) {
    BookmarksRepository repository = new BookmarksRepository();
  
    Context context = new MainActivity().getApplicationContext();
    repository.createSession(context, delegate, lastSyncTimestamp);
    testWaiter.performWait();
    return repository;
  }

  protected void prepEmptySession() {
    this.prepareRepositorySession(new SetupDelegate(), 0);
  
    // Ensure there are no records.
    session.guidsSince(0, new ExpectNoGUIDsSinceDelegate());
    testWaiter.performWait();
  }
}