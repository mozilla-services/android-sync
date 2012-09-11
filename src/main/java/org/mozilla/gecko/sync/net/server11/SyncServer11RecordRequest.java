/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.net.server11;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.GlobalConstants;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;

import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.params.CoreProtocolPNames;

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
public class SyncServer11RecordRequest {
  public static final String LOG_TAG = "SyncS11RecordRequest";

  public SyncServer11RequestDelegate delegate;

  protected BaseResourceDelegate resourceDelegate;
  protected BaseResource resource;

  /**
   * @param uri
   */
  public SyncServer11RecordRequest(URI uri) {
    this.resource = new BaseResource(uri);
    this.resource.delegate = this.makeResourceDelegate(this);
  }

  protected volatile boolean aborting = false;

  /**
   * Instruct the request that it should process no more records,
   * and decline to notify any more delegate callbacks.
   */
  public void abort() {
    aborting = true;
    try {
      this.resource.abort();
    } catch (Exception e) {
      // Just in case.
      Logger.warn(LOG_TAG, "Got exception in abort: " + e);
    }
  }

  /**
   * A ResourceDelegate that mediates between Resource-level notifications and the SyncStorageRequest.
   */
  protected static class SyncStorageResourceDelegate extends BaseResourceDelegate {
    private static final String LOG_TAG = "SSResourceDelegate";
    protected SyncServer11RecordRequest request;

    SyncStorageResourceDelegate(SyncServer11RecordRequest request) {
      super();
      this.request = request;
    }

    @Override
    public String getCredentials() {
      return this.request.delegate.credentials();
    }

    @Override
    public void handleHttpResponse(HttpResponse response) {
      if (this.request.aborting) {
        return;
      }

      Logger.debug(LOG_TAG, "SyncStorageResourceDelegate handling response: " + response.getStatusLine() + ".");
      SyncServer11RequestDelegate d = this.request.delegate;
      SyncServer11Response res = new SyncServer11Response(response);
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

      // Clients can use their delegate interface to specify X-If-Unmodified-Since.
      String ifUnmodifiedSince = this.request.delegate.ifUnmodifiedSince();
      if (ifUnmodifiedSince != null) {
        Logger.debug(LOG_TAG, "Making request with X-If-Unmodified-Since = " + ifUnmodifiedSince);
        request.setHeader("x-if-unmodified-since", ifUnmodifiedSince);
      }
      if (request.getMethod().equalsIgnoreCase("DELETE")) {
        request.addHeader("x-confirm-delete", "1");
      }
    }
  }

  public void get() {
    this.resource.get();
  }

  protected BaseResourceDelegate makeResourceDelegate(SyncServer11RecordRequest request) {
    return new SyncStorageResourceDelegate(request);
  }

  public void delete() {
    this.resource.delete();
  }

  public void post(HttpEntity body) {
    this.resource.post(body);
  }

  public void put(HttpEntity body) {
    this.resource.put(body);
  }

  public void post(JSONObject body) {
    this.resource.post(body);
  }

  public void post(JSONArray body) {
    this.resource.post(body);
  }

  public void put(JSONObject body) {
    this.resource.put(body);
  }

  public void post(CryptoRecord record) {
    this.post(record.toJSONObject());
  }

  public void put(CryptoRecord record) {
    this.put(record.toJSONObject());
  }
}
