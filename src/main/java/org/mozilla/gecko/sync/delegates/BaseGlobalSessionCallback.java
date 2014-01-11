/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.delegates;

import java.net.URI;

import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;

public interface BaseGlobalSessionCallback {
  /**
   * Request that no further syncs occur within the next `backoff` milliseconds.
   * @param backoff a duration in milliseconds.
   */
  void requestBackoff(long backoff);

  /**
   * Called on a 401 HTTP response.
   */
  void informUnauthorizedResponse(GlobalSession globalSession, URI oldClusterURL);


  /**
   * Called when an HTTP failure indicates that a software upgrade is required,
   * or the account has been EOLed.
   *
   * @param session
   *          the current session.
   * @param untilUpgraded
   *          if this is true, the account will be automatically re-enabled
   *          after this software has been upgraded.
   */
  void disableAccount(GlobalSession session, boolean untilUpgraded);

  void handleAborted(GlobalSession globalSession, String reason);
  void handleError(GlobalSession globalSession, Exception ex);
  void handleSuccess(GlobalSession globalSession);
  void handleStageCompleted(Stage currentState, GlobalSession globalSession);

  boolean shouldBackOff();
}
