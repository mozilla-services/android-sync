/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.picl.sync.net;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;

import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.sync.SyncConstants;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;
import org.mozilla.gecko.sync.net.SyncStorageRequest;
import org.mozilla.gecko.sync.net.SyncStorageRequestDelegate;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.message.BasicHeader;
import ch.boye.httpclientandroidlib.params.CoreProtocolPNames;
import ch.boye.httpclientandroidlib.protocol.BasicHttpContext;

public  class PICLStorageRequest extends SyncStorageRequest {
  public PICLStorageRequest(URI u) {
    super(u);
  }

  @Override
  protected BaseResourceDelegate makeResourceDelegate(SyncStorageRequest request) {
    return new PICLStorageResourceDelegate(request);
  }

  public static class PICLStorageResourceDelegate extends BaseResourceDelegate {

    private static final String LOG_TAG = "PICLStorageResourceDelegate";
    public SyncStorageRequest request;

    PICLStorageResourceDelegate(SyncStorageRequest request) {
      super(request);
      this.request = request;
    }

    @Override
    public AuthHeaderProvider getAuthHeaderProvider() {
      String credentials = request.delegate.credentials();
      if (credentials == null) {
        return null;
      }

      return new TokenAuthHeaderProvider(credentials);
    }

    @Override
    public void handleHttpResponse(HttpResponse response) {
      Logger.debug(LOG_TAG, "handling response: " + response.getStatusLine() + ".");
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
      client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, SyncConstants.SYNC_USER_AGENT);

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

    protected static class TokenAuthHeaderProvider implements AuthHeaderProvider {

      private String token;

      public TokenAuthHeaderProvider(String token) {
        this.token = token;
      }

      @Override
      public Header getAuthHeader(HttpRequestBase request,
          BasicHttpContext context, DefaultHttpClient client)
          throws GeneralSecurityException {
        // TODO Auto-generated method stub
        return new BasicHeader("Authorizaton", token);
      }

    }

  }
}
