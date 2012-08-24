/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.net;

public interface SyncStorageRequestDelegate {
  String credentials();

  /**
   * This header may be added to any GET request, set to a timestamp. If the
   * target resource has not been modified since the timestamp given, then a
   * <b>304 Not Modified</b> response will be returned and re-transmission of
   * the unchanged data will be avoided.
   * <p>
   * It is similar to the standard HTTP <b>If-Modified-Since</b> header, but the
   * value is expressed in integer milliseconds for extra precision.
   * <p>
   * If the value of this header is not a valid integer, or if the
   * <b>X-If-Unmodified-Since</b> header is also present, then a <b>400 Bad
   * Request</b> response will be returned.
   *
   * @return timestamp in milliseconds since the epoch, or null for none.
   */
  Long ifModifiedSince();

  /**
   * This header may be added to any request to a collection or item, set to a
   * timestamp. If the resource to be acted on has been modified since the
   * timestamp given, the request will fail with a <b>412 Precondition
   * Failed</b> response.
   * <p>
   * It is similar to the standard HTTP <b>If-Unmodified-Since</b> header, but
   * the value is expressed in integer milliseconds for extra precision.
   * <p>
   * If the value of this header is not a valid integer, or if the
   * <b>X-If-Modified-Since header</b> is also present, then a <b>400 Bad
   * Request</b> response will be returned.
   *
   * @return timestamp in milliseconds since the epoch, or null for none.
   */
  Long ifUnmodifiedSince();

  // TODO: at this point we can access X-Weave-Timestamp, compare
  // that to our local timestamp, and compute an estimate of clock
  // skew. Bug 721887.

  /**
   * Override this to handle a successful SyncStorageRequest.
   *
   * SyncStorageResourceDelegate implementers <b>must</b> ensure that the HTTP
   * responses underlying SyncStorageResponses are fully consumed to ensure that
   * connections are returned to the pool, for example by calling
   * <code>BaseResource.consumeEntity(response)</code>.
   */
  void handleRequestSuccess(SyncStorageResponse response);

  /**
   * Override this to handle a failed SyncStorageRequest.
   *
   *
   * SyncStorageResourceDelegate implementers <b>must</b> ensure that the HTTP
   * responses underlying SyncStorageResponses are fully consumed to ensure that
   * connections are returned to the pool, for example by calling
   * <code>BaseResource.consumeEntity(response)</code>.
   */
  void handleRequestFailure(SyncStorageResponse response);

  void handleRequestError(Exception ex);
}
