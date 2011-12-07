package org.mozilla.android.sync.repositories;

import org.mozilla.android.sync.SyncException;

public class InvalidBookmarkTypeException extends SyncException {

  private static final long serialVersionUID = -6098516814844387449L;
  
  public InvalidBookmarkTypeException(Exception e) {
    super(e);
  }

}
