package org.mozilla.android.sync.test;

import org.mozilla.android.sync.repositories.bookmarks.BookmarksRepository;
import org.mozilla.android.sync.repositories.bookmarks.BookmarksRepositorySession;
import org.mozilla.android.sync.test.helpers.DefaultSessionCreationDelegate;
import org.mozilla.android.sync.test.helpers.ExpectNoGUIDsSinceDelegate;
import org.mozilla.android.sync.test.helpers.WaitHelper;

import android.app.Activity;
import android.content.Context;

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

  public static BookmarksRepository prepareRepositorySession(Activity activity, DefaultSessionCreationDelegate delegate, long lastSyncTimestamp) {
    BookmarksRepository repository = new BookmarksRepository();
  
    Context context = activity.getApplicationContext();
    repository.createSession(context, delegate, lastSyncTimestamp);
    WaitHelper.getTestWaiter().performWait();
    return repository;
  }

  public static void prepEmptySession(Activity activity) {
    prepareRepositorySession(activity, new SetupDelegate(), 0);
  
    // Ensure there are no records.
    session.guidsSince(0, new ExpectNoGUIDsSinceDelegate());
    WaitHelper.getTestWaiter().performWait();
  }
}