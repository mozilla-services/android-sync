/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.net.server11;

public interface SyncServer11RequestDelegate {
  String credentials();
  String ifUnmodifiedSince();

  // TODO: at this point we can access X-Weave-Timestamp, compare
  // that to our local timestamp, and compute an estimate of clock
  // skew. Bug 721887.

  /**
   * Override this to handle a successful SyncStorageRequest.
   *
   * SyncStorageResourceDelegate implementers <b>must</b> ensure that the HTTP
   * responses underlying the Sync responses are fully consumed to ensure that
   * connections are returned to the pool, for example by calling
   * <code>BaseResource.consumeEntity(response)</code>.
   */
  void handleRequestSuccess(SyncServer11Response response);

  /**
   * Override this to handle a failed SyncStorageRequest.
   *
   *
   * SyncStorageResourceDelegate implementers <b>must</b> ensure that the HTTP
   * responses underlying the Sync responses are fully consumed to ensure that
   * connections are returned to the pool, for example by calling
   * <code>BaseResource.consumeEntity(response)</code>.
   */
  void handleRequestFailure(SyncServer11Response response);

  void handleRequestError(Exception ex);
}
