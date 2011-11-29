package org.mozilla.android.sync.test;

import org.mozilla.android.sync.repositories.bookmarks.BookmarksRepository;
import org.mozilla.android.sync.repositories.bookmarks.BookmarksRepositorySession;
import org.mozilla.android.sync.test.helpers.DefaultRepositorySessionDelegate;
import org.mozilla.android.sync.test.helpers.DefaultSessionCreationDelegate;
import org.mozilla.android.sync.test.helpers.ExpectNoGUIDsSinceDelegate;
import org.mozilla.android.sync.test.helpers.WaitHelper;

import android.content.Context;
import android.util.Log;

public class AndroidBookmarksTestHelper {

  public static WaitHelper testWaiter = WaitHelper.getTestWaiter();
  public static BookmarksRepositorySession session;
  private static AndroidBookmarksTestHelper helper;
  
  public static AndroidBookmarksTestHelper getHelper() {
    if (helper == null) {
      helper = new AndroidBookmarksTestHelper();
    }
    return helper;
  }

  private AndroidBookmarksTestHelper() {
    super();
  }

  public static BookmarksRepository prepareRepositorySession(Context context, DefaultSessionCreationDelegate delegate, long lastSyncTimestamp) {
    BookmarksRepository repository = new BookmarksRepository();
    try {
      repository.createSession(context, delegate, lastSyncTimestamp);
      Log.i("rnewman", "Calling wait.");
      testWaiter.performWait();
    } catch (IllegalArgumentException ex) {
      Log.w("prepareRepositorySession", "Caught IllegalArgumentException.");
    }
    session.begin(new DefaultRepositorySessionDelegate());

    return repository;
  }

  public static void prepEmptySession(Context context) {
    prepareRepositorySession(context, new SetupDelegate(), 0);
  
    // Ensure there are no records.
    session.guidsSince(0, new ExpectNoGUIDsSinceDelegate());
    testWaiter.performWait();
  }
}