package org.mozilla.android.sync.stage;

import org.mozilla.android.sync.GlobalSession;

public class CompletedStage implements GlobalSyncStage {

  @Override
  public void execute(GlobalSession session) throws NoSuchStageException {
    // TODO: Update tracking timestamps, close connections, etc.
    session.completeSync();
  }

}
