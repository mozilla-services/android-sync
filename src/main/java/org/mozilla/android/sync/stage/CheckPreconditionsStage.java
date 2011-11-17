package org.mozilla.android.sync.stage;

import org.mozilla.android.sync.GlobalSession;

public class CheckPreconditionsStage implements GlobalSyncStage {
  public void execute(GlobalSession session) throws NoSuchStageException {
    session.advance();
  }

}
