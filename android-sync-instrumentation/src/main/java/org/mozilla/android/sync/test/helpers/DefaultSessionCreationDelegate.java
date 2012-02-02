/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;

public class DefaultSessionCreationDelegate extends DefaultDelegate implements
    RepositorySessionCreationDelegate {

  @Override
  public void onSessionCreateFailed(Exception ex) {
    sharedFail("Should not fail.");
  }

  @Override
  public void onSessionCreated(RepositorySession session) {
    sharedFail("Should not have been created.");
  }

  @Override
  public RepositorySessionCreationDelegate deferredCreationDelegate() {
    return new RepositorySessionCreationDelegate() {
      final RepositorySessionCreationDelegate self = this;

      @Override
      public void onSessionCreated(final RepositorySession session) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            self.onSessionCreated(session);
          }
        }).start();
      }

      @Override
      public void onSessionCreateFailed(final Exception ex) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            self.onSessionCreateFailed(ex);
          }
        }).start();
      }

      @Override
      public RepositorySessionCreationDelegate deferredCreationDelegate() {
        return this;
      }
    };
  }
}
