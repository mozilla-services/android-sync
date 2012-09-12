/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.net;

import java.net.URI;
import java.net.URISyntaxException;

import org.mozilla.gecko.sync.CredentialsSource;
import org.mozilla.gecko.sync.CryptoRecord;

/**
 * Resource class that implements expected headers and processing for Sync.
 * Accepts a simplified delegate.
 *
 * Includes:
 * * Basic Auth headers (via Resource)
 * * Error responses:
 *   * 401
 *   * 503
 * * Headers:
 *   * Retry-After
 *   * X-Weave-Backoff
 *   * X-Weave-Records?
 *   * ...
 * * Timeouts
 * * Network errors
 * * application/newlines
 * * JSON parsing
 * * Content-Type and Content-Length validation.
 */
public class SyncStorageRecordRequest extends SyncStorageRequest {

  public class SyncStorageRecordResourceDelegate extends SyncStorageResourceDelegate {
    SyncStorageRecordResourceDelegate(SyncStorageRequest request) {
      super(request);
    }
  }

  public SyncStorageRecordRequest(URI uri, CredentialsSource credentialsSource) {
    super(uri, credentialsSource);
  }

  public SyncStorageRecordRequest(String url, CredentialsSource credentialsSource) throws URISyntaxException {
    this(new URI(url), credentialsSource);
  }

  @Override
  protected SyncResourceDelegate makeResourceDelegate(SyncStorageRequest request) {
    return new SyncStorageRecordResourceDelegate(request);
  }

  public void post(CryptoRecord record) {
    this.resource.post(record.toJSONObject());
  }

  public void put(CryptoRecord record) {
    this.resource.put(record.toJSONObject());
  }
}
