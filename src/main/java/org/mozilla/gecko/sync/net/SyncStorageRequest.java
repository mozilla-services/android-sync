/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.net;

import java.net.URI;
import java.net.URISyntaxException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mozilla.gecko.sync.CredentialsSource;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.Utils;

import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.client.methods.HttpDelete;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.client.methods.HttpPut;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;

/**
 * An abstract HTTP request to a Sync server.
 * <p>
 * All Sync requests compose:
 * <ul>
 * <li>an underlying HTTP request;</li>
 * <li>credentials;</li>
 * <li>locally modified version timestamp;</li>
 * <li>a simplified delegate.</li>
 * </ul>
 */
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
   * Make a delegate that turns <code>BaseResourceDelegate</code> callbacks into
   * <code>SyncStorageRequestDelegate</code> callbacks.
   *
   * @param request to delegate for.
   * @return delegate.
   */
  protected abstract BaseResourceDelegate makeResourceDelegate(SyncStorageRequest request);

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
