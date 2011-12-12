package org.mozilla.gecko.sync.stage;

import org.mozilla.gecko.sync.GlobalSession;

public class CheckPreconditionsStage implements GlobalSyncStage {
  public void execute(GlobalSession session) throws NoSuchStageException {
    session.advance();
  }
}
