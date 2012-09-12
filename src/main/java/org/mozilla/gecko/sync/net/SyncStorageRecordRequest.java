/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.net;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

import org.mozilla.gecko.sync.CredentialsSource;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.GlobalConstants;
import org.mozilla.gecko.sync.Logger;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.HttpStatus;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.params.CoreProtocolPNames;

/**
 * An HTTP request to a Sync 1.1 server.
 * <p>
 * Implements expected headers and processing for Sync protocol 1.1.
 * <p>
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
 *
 * @see <a href="http://docs.services.mozilla.com/storage/apis-1.1.html">the Sync 1.1 API documentation</a>.
 */
public class SyncStorageRecordRequest extends SyncStorageRequest {
  public SyncStorageRecordRequest(URI uri, CredentialsSource credentialsSource) {
    super(uri, credentialsSource);
  }

  public SyncStorageRecordRequest(String url, CredentialsSource credentialsSource) throws URISyntaxException {
    this(new URI(url), credentialsSource);
  }

  @Override
  protected BaseResourceDelegate makeResourceDelegate(SyncStorageRequest request) {
    return new SyncStorageRecordResourceDelegate(request);
  }

  public void post(CryptoRecord record) {
    this.resource.post(record.toJSONObject());
  }

  public void put(CryptoRecord record) {
    this.resource.put(record.toJSONObject());
  }

  /**
   * A delegate that forwards callbacks to a <code>SyncStorageRequest</code>.
   */
  protected static class SyncStorageRecordResourceDelegate extends BaseResourceDelegate {
    private static final String LOG_TAG = "SSCollResDelegate";

    protected SyncStorageRequest request;

    SyncStorageRecordResourceDelegate(SyncStorageRequest request) {
      super(request);
      this.request = request;
    }

    @Override
    public String getCredentials() {
      if (this.request.credentialsSource == null) {
        return null;
      }
      return this.request.credentialsSource.credentials();
    }

    @Override
    public void handleHttpResponse(HttpResponse response) {
      if (this.request.aborting) {
        return;
      }

      Logger.debug(LOG_TAG, "SyncStorageResourceDelegate handling response: " + response.getStatusLine() + ".");
      SyncStorageRequestDelegate d = this.request.delegate;
      SyncStorageResponse res = new SyncStorageResponse(response);
      // It is the responsibility of the delegate handlers to completely consume the response.
      if (HttpStatus.SC_OK == res.getStatusCode()) {
        d.handleRequestSuccess(res);
      } else {
        Logger.warn(LOG_TAG, "HTTP request failed.");
        try {
          Logger.warn(LOG_TAG, "HTTP response body: " + res.getErrorMessage());
        } catch (Exception e) {
          Logger.error(LOG_TAG, "Can't fetch HTTP response body.", e);
        }
        d.handleRequestFailure(res);
      }
    }

    @Override
    public void handleHttpProtocolException(ClientProtocolException e) {
      this.request.delegate.handleRequestError(e);
    }

    @Override
    public void handleHttpIOException(IOException e) {
      this.request.delegate.handleRequestError(e);
    }

    @Override
    public void handleTransportException(GeneralSecurityException e) {
      this.request.delegate.handleRequestError(e);
    }

    @Override
    public void addHeaders(HttpRequestBase request, DefaultHttpClient client) {
      client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, GlobalConstants.USER_AGENT);

      this.request.addHeaders(request, client);
    }
  }
}
