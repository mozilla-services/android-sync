package org.mozilla.gecko.sync.synchronizer;

import org.mozilla.gecko.sync.SyncException;
import org.mozilla.gecko.sync.repositories.RepositorySession;

public class UnbundleError extends SyncException {
  private static final long serialVersionUID = -8709503281041697522L;

  public RepositorySession failedSession;

  public UnbundleError(Exception e, RepositorySession session) {
    super(e);
    this.failedSession = session;
  }
}
