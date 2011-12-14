package org.mozilla.gecko.sync.repositories;

import org.mozilla.gecko.sync.SyncException;

public class ParentNotFoundException extends SyncException {

  private static final long serialVersionUID = -2687003621705922982L;

  public ParentNotFoundException(Exception ex) {
    super(ex);
  }

}
