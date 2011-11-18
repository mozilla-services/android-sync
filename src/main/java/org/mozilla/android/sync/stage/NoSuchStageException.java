package org.mozilla.android.sync.stage;

public class NoSuchStageException extends Exception {
  private static final long serialVersionUID = 8338484472880746971L;
  GlobalSyncStage.Stage stage;
  public NoSuchStageException(GlobalSyncStage.Stage stage) {
    this.stage = stage;
  }
}
