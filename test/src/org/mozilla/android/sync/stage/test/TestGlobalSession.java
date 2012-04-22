/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.stage.test;

import java.io.IOException;
import java.net.URI;

import org.json.simple.parser.ParseException;
import org.mozilla.android.sync.test.AndroidSyncTestCase;
import org.mozilla.android.sync.test.helpers.RealPrefsMockGlobalSession;
import org.mozilla.gecko.sync.AlreadySyncingException;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.delegates.GlobalSessionCallback;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;

import android.content.Context;

public class TestGlobalSession extends AndroidSyncTestCase {
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

    @Override
    public void requestBackoff(long backoff) {
      fail("No requestBackoff.");
    }

    @Override
    public void informNodeAuthenticationFailed(GlobalSession session, URI clusterURL) {
      fail("Not expecting informNodeAuthenticationFailed.");
    }

    @Override
    public void informNodeAssigned(GlobalSession session, URI oldClusterURL, URI newClusterURL) {
      fail("Not expecting informNodeReassigned.");
    }

    @Override
    public void informUnauthorizedResponse(GlobalSession session, URI clusterURL) {
      fail("Not expecting informUnauthorizedResponse.");
    }

    @Override
    public void handleAborted(GlobalSession globalSession, String reason) {
      fail("Not expecting abort.");
    }

    @Override
    public boolean shouldBackOff() {
      return false;
    }

    @Override
    public boolean wantNodeAssignment() {
      return false;
    }
  }

  public void testCallbacks() throws CryptoException, SyncConfigurationException, IllegalArgumentException, NonObjectJSONException, IOException, ParseException, AlreadySyncingException {
    BaseResource.rewriteLocalhost = false;
    String clusterURL = "http://localhost:8080/";
    String username   = "johndoe";
    String password   = "password";
    String syncKey    = "abcdeabcdeabcdeabcdeabcdea";
    KeyBundle syncKeyBundle = new KeyBundle(username, syncKey);
    HappyCallback callback = new HappyCallback();
    Context context = getApplicationContext();
    System.out.println("Using context " + context);
    GlobalSession session = new RealPrefsMockGlobalSession(clusterURL, username, password, syncKeyBundle, callback, context);
    session.start();
    assertTrue(callback.calledSuccess);
  }
}
