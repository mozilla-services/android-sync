/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.android.sync.test.helpers.DefaultSessionCreationDelegate;
import org.mozilla.android.sync.test.helpers.ExpectBeginDelegate;
import org.mozilla.android.sync.test.helpers.ExpectOnlySpecialFoldersDelegate;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.repositories.InactiveSessionException;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepositorySession;

import android.content.Context;
import android.util.Log;

public class AndroidBrowserRepositoryTestHelper {
  
  public static WaitHelper testWaiter = WaitHelper.getTestWaiter();
  public static AndroidBrowserRepositorySession session;
  
  
  public static AndroidBrowserRepository prepareRepositorySession(final Context context,
      final DefaultSessionCreationDelegate delegate,
      boolean begin,
      final AndroidBrowserRepository repository) {
    
    try {
      Runnable runnable = new Runnable() {
        @Override
        public void run() {
          repository.createSession(delegate, context);
        }
      };
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
    prepareRepositorySession(context, new SetupDelegate(), true, repository);
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        session.guidsSince(0, new ExpectOnlySpecialFoldersDelegate());
      }
    };
    testWaiter.performWait(runnable);
  }
  
  public static void prepEmptySessionWithoutBegin(Context context, AndroidBrowserRepository repository) {
    prepareRepositorySession(context, new SetupDelegate(), false, repository);
  }

}
