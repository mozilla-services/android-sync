/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.net;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.GeneralSecurityException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.GlobalConstants;
import org.mozilla.gecko.sync.Logger;

import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.entity.StringEntity;
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
public class SyncStorageRecordRequest {
  public static final String LOG_TAG = "SyncStorageRecordRequest";

  protected static StringEntity stringEntity(String s)
      throws UnsupportedEncodingException {
        StringEntity e = new StringEntity(s, "UTF-8");
        e.setContentType("application/json");
        return e;
      }

  /**
   * Helper for turning a JSON object into a payload.
   * @throws UnsupportedEncodingException
   */
  protected static StringEntity jsonEntity(JSONObject body)
      throws UnsupportedEncodingException {
        return stringEntity(body.toJSONString());
      }

  /**
   * Helper for turning a JSON array into a payload.
   * @throws UnsupportedEncodingException
   */
  protected static HttpEntity jsonEntity(JSONArray toPOST)
      throws UnsupportedEncodingException {
        return stringEntity(toPOST.toJSONString());
      }

  public SyncStorageRequestDelegate delegate;

  protected BaseResourceDelegate resourceDelegate;
  protected BaseResource resource;

  /**
   * @param uri
   */
  public SyncStorageRecordRequest(URI uri) {
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
      this.resource.request.abort();
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
    protected SyncStorageRecordRequest request;

    SyncStorageResourceDelegate(SyncStorageRecordRequest request) {
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
      SyncStorageRequestDelegate d = this.request.delegate;
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

  protected BaseResourceDelegate makeResourceDelegate(SyncStorageRecordRequest request) {
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

  @SuppressWarnings("unchecked")
  public void post(JSONObject body) {
    // Let's do this the trivial way for now.
    // Note that POSTs should be an array, so we wrap here.
    final JSONArray toPOST = new JSONArray();
    toPOST.add(body);
    try {
      this.resource.post(jsonEntity(toPOST));
    } catch (UnsupportedEncodingException e) {
      this.delegate.handleRequestError(e);
    }
  }

  public void post(JSONArray body) {
    // Let's do this the trivial way for now.
    try {
      this.resource.post(jsonEntity(body));
    } catch (UnsupportedEncodingException e) {
      this.delegate.handleRequestError(e);
    }
  }

  public void put(JSONObject body) {
    // Let's do this the trivial way for now.
    try {
      this.resource.put(jsonEntity(body));
    } catch (UnsupportedEncodingException e) {
      this.delegate.handleRequestError(e);
    }
  }

  public void post(CryptoRecord record) {
    this.post(record.toJSONObject());
  }

  public void put(CryptoRecord record) {
    this.put(record.toJSONObject());
  }
}
