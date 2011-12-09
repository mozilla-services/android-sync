package org.mozilla.android.sync.synchronizer;

import org.mozilla.android.sync.SyncException;
import org.mozilla.android.sync.repositories.RepositorySession;

public class UnbundleError extends SyncException {
  private static final long serialVersionUID = -8709503281041697522L;

  public RepositorySession failedSession;

  public UnbundleError(Exception e, RepositorySession session) {
    super(e);
    this.failedSession = session;
  }
}
