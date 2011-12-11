package org.mozilla.gecko.sync.repositories;

import org.mozilla.gecko.sync.SyncException;

public class ProfileDatabaseException extends SyncException {

  private static final long serialVersionUID = -4916908502042261602L;

  public ProfileDatabaseException(Exception ex) {
    super(ex);
  }

}
