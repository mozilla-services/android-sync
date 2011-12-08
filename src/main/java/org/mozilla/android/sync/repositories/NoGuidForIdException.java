package org.mozilla.android.sync.repositories;

import org.mozilla.android.sync.SyncException;

public class NoGuidForIdException extends SyncException {

  private static final long serialVersionUID = -675614284405829041L;

  public NoGuidForIdException(Exception ex) {
    super(ex);
  }
}
