package org.mozilla.android.sync;

import org.mozilla.android.sync.stage.GlobalSyncStage.Stage;

public class AlreadySyncingException extends SyncException {
  Stage inState;
  public AlreadySyncingException(Stage currentState) {
    inState = currentState;
  }

  private static final long serialVersionUID = -5647548462539009893L;
}
