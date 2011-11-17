package org.mozilla.android.sync.stage;

import org.mozilla.android.sync.GlobalSession;

public class EnsureClusterURLStage implements GlobalSyncStage {
  public void execute(GlobalSession session) throws NoSuchStageException {
    if (session.getClusterURL() != null) {
      session.advance();
      return;
    }
    // TODO: fetch clusterURL.
    session.advance();
  }

}
