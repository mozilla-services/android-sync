/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.net;

public abstract class SyncServer11CollectionRequestDelegate implements SyncServer11RequestDelegate {
  /**
   * Called once for each line returned by the server.
   * <p>
   * If this throws an exception, the incremental request is terminated
   * immediately; see {@link SyncServer11CollectionRequest#handleHttpResponse}.
   *
   * @param progress
   *          line returned by the server.
   * @throws Exception
   */
  public abstract void handleRequestProgress(String progress) throws Exception;
}