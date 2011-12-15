package org.mozilla.gecko.sync.repositories.delegates;

import org.mozilla.gecko.sync.repositories.RepositorySession;

public abstract class DeferrableRepositorySessionCreationDelegate implements RepositorySessionCreationDelegate {
  @Override
  public RepositorySessionCreationDelegate deferredCreationDelegate() {
    final RepositorySessionCreationDelegate self = this;
    return new RepositorySessionCreationDelegate() {

      @Override
      public void onSessionCreated(final RepositorySession session) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            self.onSessionCreated(session);
          }}).start();
      }

      @Override
      public void onSessionCreateFailed(final Exception ex) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            self.onSessionCreateFailed(ex);
          }}).start();
      }

      @Override
      public RepositorySessionCreationDelegate deferredCreationDelegate() {
        return this;
      }
    };
  }
}