package org.mozilla.android.sync.stage;

import org.mozilla.android.sync.GlobalSession;

public class CheckPreconditionsStage implements GlobalSyncStage {
  public void execute(GlobalSession session) {
    GlobalSession.advance(GlobalSyncStage.Stage.ensureClusterURL);
  }

}
