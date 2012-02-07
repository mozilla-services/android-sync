package org.mozilla.gecko.sync.jpake.stage;

import org.mozilla.gecko.sync.jpake.JPakeClient;

import android.util.Log;

public class CompleteStage implements JPakeStage {
  private final String LOG_TAG = "CompleteStage";

  @Override
  public void execute(JPakeClient jClient) {
    Log.d(LOG_TAG, "Exchange complete.");
    jClient.finished = true;
    jClient.complete(jClient.jCreds);
    Log.d(LOG_TAG, "Advancing stage.");
    jClient.runNextStage();
  }
}
