package org.mozilla.android.sync;

import org.mozilla.android.sync.stage.GlobalSyncStage.Stage;


public interface GlobalSessionCallback {
  void handleError(GlobalSession globalSession, Exception ex);
  void handleSuccess(GlobalSession globalSession);
  void handleStageCompleted(Stage currentState, GlobalSession globalSession);
}
