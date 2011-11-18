package org.mozilla.android.sync.test;

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.android.sync.MainActivity;
import org.mozilla.android.sync.repositories.BookmarksRepository;
import org.mozilla.android.sync.repositories.BookmarksRepositorySession;
import org.mozilla.android.sync.repositories.CollectionType;
import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.SyncCallbackReceiver;

import android.content.Context;

import com.xtremelabs.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TestAndroidBookmarksRepo {

  private BookmarksRepositorySession session;

  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void testCreateSession() {

    BookmarksRepository repo = new BookmarksRepository(CollectionType.Bookmarks);
    SyncCallbackReceiver callback = new Callback();
    Context context = new MainActivity().getApplicationContext();
    repo.createSession(context, callback);

    // TODO right now this might work because we aren't using threads...
    // but once we have threads need to figure out how to test with them
    if (getSession() == null) fail();

  }

  @Test
  public void testGuidsSince() {
    // Hmmm...we need to do this for every test... look into that setup method to see
    // if that's the answer
    BookmarksRepository repo = new BookmarksRepository(CollectionType.Bookmarks);
    SyncCallbackReceiver callback = new Callback();
    Context context = new MainActivity().getApplicationContext();
    repo.createSession(context, callback);

    BookmarkSessionTestWrapper testWrapper = new BookmarkSessionTestWrapper();
    GuidsSinceTestResult result = testWrapper.doGuidsSince(session, (System.currentTimeMillis() - 1000000)/1000);
    System.out.println(result.getStatusCode());
    System.out.println(result.getGuids());
  }

  public BookmarksRepositorySession getSession() {
    return session;
  }

  public void setSession(BookmarksRepositorySession session) {
    this.session = session;
  }

  class Callback implements SyncCallbackReceiver {

    public void sessionCallback(RepoStatusCode error, RepositorySession session) {
      if (error == RepoStatusCode.DONE) {
        setSession((BookmarksRepositorySession) session);
      }

    }

    public void storeCallback(RepoStatusCode error) {
      // TODO Auto-generated method stub

    }

  }

}
