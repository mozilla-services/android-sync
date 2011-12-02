/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.android.sync.repositories.android.AndroidBrowserRepository;
import org.mozilla.android.sync.repositories.android.AndroidBrowserRepositorySession;
import org.mozilla.android.sync.test.helpers.DefaultSessionCreationDelegate;
import org.mozilla.android.sync.test.helpers.ExpectBeginDelegate;
import org.mozilla.android.sync.test.helpers.ExpectNoGUIDsSinceDelegate;
import org.mozilla.android.sync.test.helpers.WaitHelper;

import android.content.Context;
import android.util.Log;

public class AndroidRepositoryTestHelper {
  
  public static WaitHelper testWaiter = WaitHelper.getTestWaiter();
  public static AndroidBrowserRepositorySession session;
  
  
  public static AndroidBrowserRepository prepareRepositorySession(final Context context,
      final DefaultSessionCreationDelegate delegate,
      final long lastSyncTimestamp,
      boolean begin,
      final AndroidBrowserRepository repository) {
    
    try {
      Runnable runnable = new Runnable() {
        @Override
        public void run() {
          repository.createSession(delegate, context, lastSyncTimestamp);
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
  
  public static void prepEmptySession(Context context, AndroidBrowserRepository repository) {
    prepareRepositorySession(context, new SetupDelegate(), 0, true, repository);
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        session.guidsSince(0, new ExpectNoGUIDsSinceDelegate());
      }
    };
    testWaiter.performWait(runnable);
  }
  
  public static void prepEmptySessionWithoutBegin(Context context, AndroidBrowserRepository repository) {
    prepareRepositorySession(context, new SetupDelegate(), 0, false, repository);
  }

}
