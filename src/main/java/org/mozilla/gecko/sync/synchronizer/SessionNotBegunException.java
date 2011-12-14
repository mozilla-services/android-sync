package org.mozilla.gecko.sync.synchronizer;

import org.mozilla.gecko.sync.SyncException;
import org.mozilla.gecko.sync.repositories.RepositorySession;

public class SessionNotBegunException extends SyncException {
  
  public RepositorySession failed;

  public SessionNotBegunException(RepositorySession failed) {
    this.failed = failed;
  }

  private static final long serialVersionUID = -4565241449897072841L;
}
