/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static junit.framework.Assert.assertNotNull;

import org.mozilla.android.sync.test.helpers.DefaultSessionCreationDelegate;
import org.mozilla.android.sync.test.helpers.ExpectBeginDelegate;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.repositories.InvalidSessionTransitionException;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.RepositorySession;

import android.content.Context;
import android.util.Log;

public class SessionTestHelper {

  public static RepositorySession prepareRepositorySession(
      final Context context,
      final boolean begin,
      final Repository repository) {

    final WaitHelper testWaiter = WaitHelper.getTestWaiter();

    final String logTag = "prepareRepositorySession";
    class CreationDelegate extends DefaultSessionCreationDelegate {
      private RepositorySession session;
      synchronized void setSession(RepositorySession session) {
        this.session = session;
      }
      synchronized RepositorySession getSession() {
        return this.session;
      }

      @Override
      public void onSessionCreated(final RepositorySession session) {
        assertNotNull(session);
        Log.i(logTag, "Setting session to " + session);
        setSession(session);
        if (begin) {
          Log.i(logTag, "Calling session.begin on new session.");
          // The begin callbacks will notify.
          try {
            session.begin(new ExpectBeginDelegate());
          } catch (InvalidSessionTransitionException e) {
            testWaiter.performNotify(e);
          }
        } else {
          Log.i(logTag, "Notifying after setting new session.");
          testWaiter.performNotify();
        }
      }
    }

    final CreationDelegate delegate = new CreationDelegate();
    try {
      Runnable runnable = new Runnable() {
        @Override
        public void run() {
          repository.createSession(delegate, context);
        }
      };
      testWaiter.performWait(runnable);
    } catch (IllegalArgumentException ex) {
      Log.w(logTag, "Caught IllegalArgumentException.");
    }

    Log.i(logTag, "Retrieving new session.");
    final RepositorySession session = delegate.getSession();
    assertNotNull(session);

    return session;
  }
  
  public static RepositorySession prepEmptySession(final Context context, final Repository repository) {
    return prepareRepositorySession(context, true, repository);
  }
  
  public static RepositorySession prepEmptySessionWithoutBegin(Context context, Repository repository) {
    return prepareRepositorySession(context, false, repository);
  }
}
