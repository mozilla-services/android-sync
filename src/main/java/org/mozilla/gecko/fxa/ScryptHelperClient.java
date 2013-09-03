/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxa;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

import org.json.simple.JSONObject;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;
import org.mozilla.gecko.sync.net.SyncResponse;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;

/**
 * HTTP client for the scrypt-helper server developed at
 * <a href="https://github.com/mozilla/scrypt-helper">https://github.com/mozilla/scrypt-helper</a>.
 * <p>
 * This implementation was written against
 * <a href="https://github.com/mozilla/scrypt-helper/commit/6f3598dbbfc7d3a923dabf2045e5dbe8fbd8cd49">https://github.com/mozilla/scrypt-helper/commit/6f3598dbbfc7d3a923dabf2045e5dbe8fbd8cd49</a>.
 */
public class ScryptHelperClient {
  public static final String LOG_TAG = ScryptHelperClient.class.getSimpleName();

  public static final String SALT = "identity.mozilla.com/picl/v1/scrypt";

  public interface ScryptHelperDelegate {
    public void onSuccess(String output);
    public void onFailure(HttpResponse response);
    public void onError(Exception e);
  }

  protected static class ScryptHelperResourceDelegate extends BaseResourceDelegate {
    public final ScryptHelperDelegate delegate;

    public ScryptHelperResourceDelegate(BaseResource resource, ScryptHelperDelegate delegate) {
      super(resource);
      if (delegate == null) {
        throw new IllegalArgumentException("delegate must not be null");
      }
      this.delegate = delegate;
    }

    @Override
    public void handleHttpResponse(HttpResponse response) {
      final SyncResponse r = new SyncResponse(response);
      final int statusCode = r.getStatusCode();
      Logger.debug(LOG_TAG, "Got scrypt helper response with status code: " + statusCode);

      if (statusCode != 200) {
        delegate.onFailure(response);
        return;
      }

      try {
        ExtendedJSONObject o = r.jsonObjectBody();
        delegate.onSuccess(o.getString("output"));
        return;
      } catch (Exception e) {
        delegate.onError(e);
        return;
      }
    }

    @Override
    public void handleHttpProtocolException(ClientProtocolException e) {
      Logger.warn(LOG_TAG, "Protocol exception.", e);
      delegate.onError(e);
    }

    @Override
    public void handleHttpIOException(IOException e) {
      Logger.warn(LOG_TAG, "IO exception.", e);
      delegate.onError(e);
    }

    @Override
    public void handleTransportException(GeneralSecurityException e) {
      Logger.warn(LOG_TAG, "Transport exception.", e);
      delegate.onError(e);
    }
  }

  public final String serverURL;

  public ScryptHelperClient(String serverURL) {
    if (serverURL == null) {
      throw new IllegalArgumentException("serverURL must not be null");
    }
    this.serverURL = serverURL;
  }

  @SuppressWarnings("unchecked")
  protected JSONObject getObject(String input, String salt, int N, int r, int p, int buflen) {
    final JSONObject o = new JSONObject();
    o.put("input", input);
    o.put("salt", salt);
    o.put("N", N);
    o.put("r", r);
    o.put("p", p);
    o.put("buflen", buflen);
    return o;
  }

  protected void scrypt(String input, String salt, int N, int r, int p, int buflen, final ScryptHelperDelegate delegate) {
    if (input == null) {
      throw new IllegalArgumentException("input must not be null");
    }
    if (salt == null) {
      throw new IllegalArgumentException("salt must not be null");
    }

    JSONObject o = getObject(input, salt, N, r, p, buflen);

    URI uri;
    try {
      uri = new URI(this.serverURL);
    } catch (URISyntaxException e) {
      delegate.onError(e);
      return;
    }

    BaseResource resource = new BaseResource(uri);
    resource.delegate = new ScryptHelperResourceDelegate(resource, delegate);

    try {
      resource.post(o);
    } catch (UnsupportedEncodingException e) {
      delegate.onError(e);
      return;
    }
  }

  public void scrypt(String input, ScryptHelperDelegate delegate) {
    scrypt(input, SALT, 65536, 8, 1, 32, delegate);
  }
}
