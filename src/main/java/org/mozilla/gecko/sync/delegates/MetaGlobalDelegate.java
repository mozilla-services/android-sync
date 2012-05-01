/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.delegates;

import org.mozilla.gecko.sync.MetaGlobal;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

public interface MetaGlobalDelegate {
  public void handleSuccess(MetaGlobal global, SyncStorageResponse response);
  /**
   * Called when server returns 404.
   * <p>
   * Only on download.
   * @param response
   */
  public void handleMissing(SyncStorageResponse response);
  /**
   * Called when server returns 200 but meta/global object could not be parsed.
   * <p>
   * Only on download.
   * @param response
   */
  public void handleMalformed(SyncStorageResponse response);
  public void handleFailure(SyncStorageResponse response);
  public void handleError(Exception e);
}
