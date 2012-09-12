/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.net;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mozilla.gecko.sync.CredentialsSource;
import org.mozilla.gecko.sync.GlobalConstants;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.Utils;

import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.methods.HttpDelete;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.client.methods.HttpPut;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.params.CoreProtocolPNames;

public abstract class SyncStorageRequest implements Resource {
  public static final String LOG_TAG = "SyncStorageRequest";

  protected volatile boolean aborting = false;

  /**
   * Set this to the timestamp of the most recent local version of the remote
   * server data to make this storage request conditional.
   * <p>
   * This timestamp is interpreted differently depending on the underlying HTTP
   * request's method; see {@link #allowIfUnmodified} and
   * {@link #allowIfModified}.
   */
  public Long locallyModifiedVersion = null;

  protected BaseResource resource;
  protected BaseResourceDelegate resourceDelegate;

  public SyncStorageRequestDelegate delegate;

  protected final CredentialsSource credentialsSource;

  /**
   * @param uri
   * @throws URISyntaxException
   */
  public SyncStorageRequest(String uri, CredentialsSource credentialsSource) throws URISyntaxException {
    this(new URI(uri), credentialsSource);
  }

  /**
   * @param uri
   */
  public SyncStorageRequest(URI uri, CredentialsSource credentialsSource) {
    this.credentialsSource = credentialsSource;
    this.resource = new BaseResource(uri);
    this.resourceDelegate = this.makeResourceDelegate(this);
    this.resource.delegate = this.resourceDelegate;
  }

  /**
   * Whether to populate the X-If-Unmodified-Since header for this storage
   * request.
   *
   * @param request
   *          underlying HTTP request.
   * @return <code>true</code> if yes; <code>false</code> otherwise.
   */
  protected boolean allowIfUnmodified(HttpRequestBase request) {
    final String method = request.getMethod();
    return method.equalsIgnoreCase(HttpPut.METHOD_NAME) ||
        method.equalsIgnoreCase(HttpPost.METHOD_NAME) ||
        method.equalsIgnoreCase(HttpDelete.METHOD_NAME);
  }

  /**
   * Whether to populate the X-If-Modified-Since header for this storage
   * request.
   *
   * @param request
   *          underlying HTTP request.
   * @return <code>true</code> if yes; <code>false</code> otherwise.
   */
  protected boolean allowIfModified(HttpRequestBase request) {
    final String method = request.getMethod();
    return method.equalsIgnoreCase(HttpGet.METHOD_NAME);
  }

  /**
   * Populate headers for the HTTP request underlying this storage request.
   *
   * @param request
   *          underlying HTTP request to populate.
   * @param client
   *          executing the underlying HTTP request.
   */
  protected void addHeaders(HttpRequestBase request, DefaultHttpClient client) {
    // Sync 1.1 header to allow deletion.
    if (request.getMethod().equalsIgnoreCase(HttpDelete.METHOD_NAME)) {
      request.addHeader("x-confirm-delete", "1");
    }

    // Sync 1.1 and 2.0 headers.
    if (locallyModifiedVersion != null) {
      final String header = Utils.millisecondsToDecimalSecondsString(locallyModifiedVersion.longValue());

      if (allowIfUnmodified(request)) {
        Logger.debug(LOG_TAG, "Making request with X-If-Unmodified-Since = " + header);
        request.setHeader("x-if-unmodified-since", header);
      } else if (allowIfModified(request)) {
        Logger.debug(LOG_TAG, "Making request with X-If-Modified-Since = " + header);
        request.setHeader("x-if-modified-since", header);
      }
    }
  }

  /**
   * Instruct the request that it should process no more records, and decline to
   * notify any more delegate callbacks.
   */
  public void abort() {
    aborting = true;
    try {
      this.resource.request.abort();
    } catch (Exception e) {
      // Just in case.
      Logger.warn(LOG_TAG, "Got exception in abort: " + e);
    }
  }

  /**
   * A ResourceDelegate that mediates between Resource-level notifications and the SyncStorageRequest.
   */
  public static class SyncStorageResourceDelegate extends BaseResourceDelegate {
    private static final String LOG_TAG = "SSResourceDelegate";
    protected SyncStorageRequest request;

    SyncStorageResourceDelegate(SyncStorageRequest request) {
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
      if (res.wasSuccessful()) {
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

  // Default implementation. Override this.
  protected BaseResourceDelegate makeResourceDelegate(SyncStorageRequest request) {
    return new SyncStorageResourceDelegate(request);
  }

  public void get() {
    this.resource.get();
  }

  public void delete() {
    this.resource.delete();
  }

  public void post(HttpEntity body) {
    this.resource.post(body);
  }

  public void post(JSONObject body) {
    this.resource.post(body);
  }

  public void post(JSONArray body) {
    this.resource.post(body);
  }

  public void put(HttpEntity body) {
    this.resource.put(body);
  }

  public void put(JSONObject body) {
    this.resource.put(body);
  }
}
