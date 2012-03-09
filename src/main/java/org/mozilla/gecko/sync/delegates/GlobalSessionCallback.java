/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Android Sync Client.
 *
 * The Initial Developer of the Original Code is
 * the Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Richard Newman <rnewman@mozilla.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.gecko.sync.delegates;

import java.net.URI;

import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;

public interface GlobalSessionCallback {

  /**
   * Request that no further syncs occur within the next `backoff` milliseconds.
   * @param backoff a duration in milliseconds.
   */
  void requestBackoff(long backoff);

  /**
   * If true, request node assignment from the server, i.e., fetch node/weave cluster URL.
   */
  boolean wantNodeAssignment();

  /**
   * Called on a 401 HTTP response.
   */
  void informMaybeNodeReassigned(GlobalSession globalSession, URI oldClusterURL);

  /**
   * Called when a new node is assigned. If there already was an old node, the
   * new node is different from the old node assignment, indicating node
   * reassignment. If there wasn't an old node, we've been freshly assigned.
   *
   * @param globalSession
   * @param oldClusterURL
   *          The old node/weave cluster URL (possibly null).
   * @param newClusterURL
   *          The new node/weave cluster URL (not null).
   */
  void informNodeAssigned(GlobalSession globalSession, URI oldClusterURL, URI newClusterURL);

  /**
   * Called when wantNodeAssignment() is true, and the new node assignment is
   * the same as the old node assignment, indicating a user authentication
   * error.
   *
   * @param globalSession
   * @param newClusterURL
   *          The new node/weave cluster URL.
   */
  void informNodeAuthenticationFailed(GlobalSession globalSession, URI failedClusterURL);

  void handleAborted(GlobalSession globalSession, String reason);
  void handleError(GlobalSession globalSession, Exception ex);
  void handleSuccess(GlobalSession globalSession);
  void handleStageCompleted(Stage currentState, GlobalSession globalSession);

  boolean shouldBackOff();
}
