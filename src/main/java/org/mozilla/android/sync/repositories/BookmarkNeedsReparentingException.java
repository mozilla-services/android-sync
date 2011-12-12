package org.mozilla.android.sync.repositories;

import org.mozilla.android.sync.SyncException;

public class BookmarkNeedsReparentingException extends SyncException {

  private static final long serialVersionUID = -7018336108709392800L;

  public BookmarkNeedsReparentingException(Exception ex) {
    super(ex);
  }

}