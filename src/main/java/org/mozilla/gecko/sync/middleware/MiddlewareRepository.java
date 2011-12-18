package org.mozilla.gecko.sync.middleware;

import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;

public abstract class MiddlewareRepository extends Repository {

  public abstract class SessionCreationDelegate implements
      RepositorySessionCreationDelegate {

    // We call through to our inner repository, so we don't need our own
    // deferral scheme.
    @Override
    public RepositorySessionCreationDelegate deferredCreationDelegate() {
      return this;
    }

  }
}
