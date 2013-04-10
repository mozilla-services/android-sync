/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

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

  public List<Stage> stagesCompleted = new LinkedList<Stage>();
  public boolean calledSuccess = false;
  public boolean calledError = false;
  public Exception calledErrorException = null;
  public boolean calledAborted = false;
  public boolean calledRequestBackoff = false;
  public boolean calledInformNodeAuthenticationFailed = false;
  public boolean calledInformNodeAssigned = false;
  public boolean calledInformUnauthorizedResponse = false;
  public boolean calledInformUpgradeRequiredResponse = false;
  public URI calledInformNodeAuthenticationFailedClusterURL = null;
  public URI calledInformNodeAssignedOldClusterURL = null;
  public URI calledInformNodeAssignedNewClusterURL = null;
  public URI calledInformUnauthorizedResponseClusterURL = null;
  public long weaveBackoff = -1;

  @Override
  public void handleSuccess(GlobalSession globalSession) {
    this.calledSuccess = true;
    this.testWaiter().performNotify();
  }

  @Override
  public void handleAborted(GlobalSession globalSession, String reason) {
    this.calledAborted = true;
    this.testWaiter().performNotify();
  }

  @Override
  public void handleError(GlobalSession globalSession, Exception ex) {
    this.calledError = true;
    this.calledErrorException = ex;
    this.testWaiter().performNotify();
  }

  @Override
  public void handleStageCompleted(Stage currentState, GlobalSession globalSession) {
    stagesCompleted.add(currentState);
  }

  @Override
  public void requestBackoff(long backoff) {
    this.calledRequestBackoff = true;
    this.weaveBackoff = backoff;
  }

  @Override
  public void informNodeAuthenticationFailed(GlobalSession session, URI clusterURL) {
    this.calledInformNodeAuthenticationFailed = true;
    this.calledInformNodeAuthenticationFailedClusterURL = clusterURL;
  }

  @Override
  public void informNodeAssigned(GlobalSession session, URI oldClusterURL, URI newClusterURL) {
    this.calledInformNodeAssigned = true;
    this.calledInformNodeAssignedOldClusterURL = oldClusterURL;
    this.calledInformNodeAssignedNewClusterURL = newClusterURL;
  }

  @Override
  public void informUnauthorizedResponse(GlobalSession session, URI clusterURL) {
    this.calledInformUnauthorizedResponse = true;
    this.calledInformUnauthorizedResponseClusterURL = clusterURL;
  }

  @Override
  public void informUpgradeRequiredResponse(GlobalSession session) {
    this.calledInformUpgradeRequiredResponse = true;
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
