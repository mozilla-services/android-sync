package org.mozilla.android.sync.repositories;

import org.mozilla.android.sync.SyncException;

public class ProfileDatabaseException extends SyncException {
  
  private static final long serialVersionUID = -4916908502042261602L;

  public ProfileDatabaseException(Exception ex) {
    super(ex);
  }

}
