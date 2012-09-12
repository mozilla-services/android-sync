/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.net;

public interface SyncStorageRequestIncrementalDelegate extends SyncStorageRequestDelegate {
  /**
   * Called once for each line returned by the server.
   * <p>
   * If this throws an exception, the incremental request is terminated
   * immediately; see {@link SyncStorageCollectionRequest#handleHttpResponse}.
   *
   * @param progress
   *          line returned by the server.
   * @throws Exception
   */
  void handleRequestProgress(String progress) throws Exception;
}