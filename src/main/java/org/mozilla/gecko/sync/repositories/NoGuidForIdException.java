package org.mozilla.gecko.sync.repositories;

import org.mozilla.gecko.sync.SyncException;

public class NoGuidForIdException extends SyncException {

  private static final long serialVersionUID = -675614284405829041L;

  public NoGuidForIdException(Exception ex) {
    super(ex);
  }
}
