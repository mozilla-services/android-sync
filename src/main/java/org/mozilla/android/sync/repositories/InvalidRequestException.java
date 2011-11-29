package org.mozilla.android.sync.repositories;

import org.mozilla.android.sync.SyncException;

public class InvalidRequestException extends SyncException {

  private static final long serialVersionUID = 4502951350743608243L;
  
  public InvalidRequestException(Exception ex) {
    super(ex);
  }

}
