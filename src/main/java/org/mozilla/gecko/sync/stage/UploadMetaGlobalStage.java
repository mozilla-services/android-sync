package org.mozilla.gecko.sync.stage;

import org.mozilla.gecko.sync.GlobalSession;

public class UploadMetaGlobalStage extends AbstractNonRepositorySyncStage {
  public static final String LOG_TAG = "UploadMGStage";

  public UploadMetaGlobalStage(GlobalSession session) {
    super(session);
  }

  @Override
  public void execute() throws NoSuchStageException {
    if (session.hasUpdatedMetaGlobal()) {
      session.uploadUpdatedMetaGlobal();
    }
    session.advance();
  }
}
