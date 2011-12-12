package org.mozilla.gecko.sync.repositories;

import org.mozilla.gecko.sync.SyncException;

public class NullCursorException extends SyncException {

  private static final long serialVersionUID = 3146506225701104661L;

  public NullCursorException(Exception e) {
    super(e);
  }

}
