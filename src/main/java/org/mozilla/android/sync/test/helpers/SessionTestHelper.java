/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import java.util.concurrent.ExecutorService;

import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.repositories.InvalidSessionTransitionException;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionBeginDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;

import android.content.Context;

public class SessionTestHelper {
  public static final String LOG_TAG = "SessionTestHelper";

  protected static RepositorySession prepareRepositorySession(
      final Context context,
      final boolean begin,
      final Repository repository) {

    class SessionTestBeginDelegate implements RepositorySessionBeginDelegate {
      public final WaitHelper testWaiter;

      public SessionTestBeginDelegate(WaitHelper testWaiter) {
        this.testWaiter = testWaiter;
      }

      @Override
      public void onBeginSucceeded(RepositorySession session) {
        testWaiter.performNotify();
      }

      @Override
      public void onBeginFailed(Exception ex) {
        testWaiter.performNotify(ex);
      }

      @Override
      public RepositorySessionBeginDelegate deferredBeginDelegate(ExecutorService executor) {
        return this;
      }
    }

    class SessionTestCreationDelegate implements RepositorySessionCreationDelegate {
      private RepositorySession session;
      private final WaitHelper testWaiter;

      public SessionTestCreationDelegate(WaitHelper testWaiter) {
        this.testWaiter = testWaiter;
      }

      synchronized void setSession(RepositorySession session) {
        this.session = session;
      }
      synchronized RepositorySession getSession() {
        return this.session;
      }

      @Override
      public void onSessionCreated(final RepositorySession session) {
        if (session == null) {
          testWaiter.performNotify(new RuntimeException("session should not be null."));
          return;
        }

        Logger.info(LOG_TAG, "Setting session to " + session);
        setSession(session);

        if (begin) {
          Logger.info(LOG_TAG, "Calling session.begin on new session.");
          // The begin callbacks will notify.
          try {
            session.begin(new SessionTestBeginDelegate(testWaiter));
          } catch (InvalidSessionTransitionException e) {
            testWaiter.performNotify(e);
          }
        } else {
          Logger.info(LOG_TAG, "Notifying after setting new session.");
          testWaiter.performNotify();
        }
      }

      @Override
      public void onSessionCreateFailed(Exception ex) {
        testWaiter.performNotify(ex);
      }

      @Override
      public RepositorySessionCreationDelegate deferredCreationDelegate() {
        return this;
      }
    }

    final WaitHelper theTestWaiter = new WaitHelper();
    final SessionTestCreationDelegate creationDelegate = new SessionTestCreationDelegate(theTestWaiter);
    try {
      Runnable runnable = new Runnable() {
        @Override
        public void run() {
          repository.createSession(creationDelegate, context);
        }
      };
      theTestWaiter.performWait(runnable);
    } catch (IllegalArgumentException ex) {
      Logger.warn(LOG_TAG, "Caught IllegalArgumentException.");
    }

    Logger.info(LOG_TAG, "Retrieving new session.");
    final RepositorySession session = creationDelegate.getSession();

    if (session == null) {
      throw new RuntimeException("session should not be null.");
    }

    return session;
  }

  public static RepositorySession createSession(final Context context, final Repository repository) {
    return prepareRepositorySession(context, false, repository);
  }

  public static RepositorySession createAndBeginSession(Context context, Repository repository) {
    return prepareRepositorySession(context, true, repository);
  }
}
