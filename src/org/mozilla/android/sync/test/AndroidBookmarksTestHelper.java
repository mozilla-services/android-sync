/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.android.sync.repositories.android.AndroidBrowserBookmarksRepository;
import org.mozilla.android.sync.repositories.android.AndroidBrowserBookmarksRepositorySession;
import org.mozilla.android.sync.test.helpers.DefaultSessionCreationDelegate;
import org.mozilla.android.sync.test.helpers.ExpectBeginDelegate;
import org.mozilla.android.sync.test.helpers.ExpectNoGUIDsSinceDelegate;
import org.mozilla.android.sync.test.helpers.WaitHelper;

import android.content.Context;
import android.util.Log;

public class AndroidBookmarksTestHelper {

  public static WaitHelper testWaiter = WaitHelper.getTestWaiter();
  public static AndroidBrowserBookmarksRepositorySession session;
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

  public static AndroidBrowserBookmarksRepository prepareRepositorySession(final Context context,
                                                             final DefaultSessionCreationDelegate delegate,
                                                             final long lastSyncTimestamp,
                                                             boolean begin) {
    final AndroidBrowserBookmarksRepository repository = new AndroidBrowserBookmarksRepository();
    try {
      Runnable runnable = new Runnable() {
        @Override
        public void run() {
          repository.createSession(context, delegate, lastSyncTimestamp);
        }
      };
      Log.i("rnewman", "Calling wait.");
      testWaiter.performWait(runnable);
    } catch (IllegalArgumentException ex) {
      Log.w("prepareRepositorySession", "Caught IllegalArgumentException.");
    }
    
    if (begin) {
      session.begin(new ExpectBeginDelegate());
    }

    return repository;
  }

  public static void prepEmptySession(Context context) {
    prepareRepositorySession(context, new SetupDelegate(), 0, true);
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        session.guidsSince(0, new ExpectNoGUIDsSinceDelegate());
      }
    };
    testWaiter.performWait(runnable);
  }
  
  public static void prepEmptySessionWithoutBegin(Context context) {
    prepareRepositorySession(context, new SetupDelegate(), 0, false);
  }
  
}