package org.mozilla.gecko.sync.stage;

import org.mozilla.gecko.sync.SyncException;

public class NoSyncIDException extends SyncException {
  public String engineName;
  public NoSyncIDException(String engineName) {
    this.engineName = engineName;
  }

  private static final long serialVersionUID = -4750430900197986797L;

}
