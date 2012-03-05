/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static org.junit.Assert.assertEquals;

import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.delegates.GlobalSessionCallback;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;

/**
 * A callback for use with a GlobalSession that records what happens for later
 * inspection.
 *
 * This callback is expected to be used from within the friendly confines of a
 * WaitHelper performWait.
 */
public class MockGlobalSessionCallback implements GlobalSessionCallback {
  protected WaitHelper testWaiter() {
    return WaitHelper.getTestWaiter();
  }

  public int stageCounter = Stage.values().length - 1; // Exclude starting state.
  public boolean calledSuccess = false;
  public boolean calledError = false;
  public boolean calledAborted = false;
  public boolean calledRequestBackoff = false;
  public long weaveBackoff = -1;

  @Override
  public void handleSuccess(GlobalSession globalSession) {
    this.calledSuccess = true;
    assertEquals(0, this.stageCounter);
    this.testWaiter().performNotify();
  }

  @Override
  public void handleAborted(GlobalSession globalSession, String reason) {
    this.calledAborted = true;
    this.testWaiter().performNotify();
  }

  @Override
  public void handleError(GlobalSession globalSession, Exception ex) {
    System.out.println("Error in MockGlobalSessionCallback.");
    ex.printStackTrace();
    this.calledError = true;
    this.testWaiter().performNotify();
  }

  @Override
  public void handleStageCompleted(Stage currentState,
           GlobalSession globalSession) {
    stageCounter--;
  }

  @Override
  public void requestBackoff(long backoff) {
    this.calledRequestBackoff = true;
    this.weaveBackoff = backoff;
  }

  @Override
  public boolean shouldBackOff() {
    return false;
  }
}
