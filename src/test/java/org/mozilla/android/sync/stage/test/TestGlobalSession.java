package org.mozilla.android.sync.stage.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.mozilla.android.sync.GlobalSession;
import org.mozilla.android.sync.GlobalSessionCallback;
import org.mozilla.android.sync.crypto.KeyBundle;
import org.mozilla.android.sync.stage.GlobalSyncStage.Stage;

public class TestGlobalSession {

  @Test
  public void testStageAdvance() {
    assertEquals(GlobalSession.nextStage(Stage.idle), Stage.checkPreconditions);
    assertEquals(GlobalSession.nextStage(Stage.completed), Stage.idle);
  }

  protected class HappyCallback implements GlobalSessionCallback {
    int stageCounter = Stage.values().length - 1;    // Exclude starting state.
    public boolean calledSuccess = false;

    public void handleError(GlobalSession globalSession, Exception ex) {
      ex.printStackTrace();
      fail("No error should occur.");
    }

    public void handleSuccess(GlobalSession globalSession) {
      assertEquals(0, stageCounter);
      calledSuccess = true;
    }

    public void handleStageCompleted(Stage currentState,
                                     GlobalSession globalSession) {
      stageCounter--;
    }

  }
  @Test
  public void testCallbacks() {
    String clusterURL = "http://localhost:8080/";
    String username   = "johndoe";
    String password   = "password";
    String syncKey    = "abcdeabcdeabcdeabcdeabcdea";
    KeyBundle syncKeyBundle = new KeyBundle(username, syncKey);
    HappyCallback callback = new HappyCallback();
    GlobalSession session;
    try {
      session = new GlobalSession(clusterURL, username, password, syncKeyBundle, callback);
      session.start();
      assertTrue(callback.calledSuccess);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Got exception.");
    }

  }
}
