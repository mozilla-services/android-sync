package org.mozilla.gecko.sync.repositories;

import org.mozilla.gecko.sync.SyncException;

public class BookmarkNeedsReparentingException extends SyncException {

  private static final long serialVersionUID = -7018336108709392800L;

  public BookmarkNeedsReparentingException(Exception ex) {
    super(ex);
  }

}