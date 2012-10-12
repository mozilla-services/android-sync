package org.mozilla.todo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.json.simple.JSONArray;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;
import org.mozilla.gecko.sync.net.Resource;
import org.mozilla.gecko.sync.net.ResourceDelegate;
import org.mozilla.gecko.sync.net.SyncResponse;

import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.CookieStore;
import ch.boye.httpclientandroidlib.client.protocol.ClientContext;
import ch.boye.httpclientandroidlib.cookie.Cookie;
import ch.boye.httpclientandroidlib.impl.client.BasicCookieStore;

/**
 * Interface to 123done.org REST API for managing todo lists.
 * <p>
 * Initial login is authenticated by BrowserID assertion, and subsequent request
 * authentication is by secure session cookie.
 * <p>
 * This is a blocking interface for test code only.
 */
public class TodoClient {
  final CookieStore cookieStore = new BasicCookieStore();

  protected final String serverURL; // = "http://localhost:8080";
  protected final String host; // = "localhost:8080";

  public TodoClient(final String serverURL) {
    this.serverURL = serverURL;
    final String[] pieces = this.serverURL.split("://"); // This must be in URI, but I can't find it.
    this.host = pieces[pieces.length-1];
  }

  public TodoClient(final String serverURL, final String host) {
    this.serverURL = serverURL;
    this.host = host;
  }

  protected ResourceDelegate makeResourceDelegate(Resource r, final boolean wantBody, final TodoClientDelegate delegate) {
    return new BaseResourceDelegate(r) {
      @Override
      public void handleHttpResponse(HttpResponse response) {
        SyncResponse res = new SyncResponse(response);

        try {
          delegate.handleResponse(res.getStatusCode(), res.body());
        } catch (Exception e) {
          delegate.handleError(e);
        }
      }

      @Override
      public void handleTransportException(GeneralSecurityException e) {
        delegate.handleError(e);
      }

      @Override
      public void handleHttpProtocolException(ClientProtocolException e) {
        delegate.handleError(e);
      }

      @Override
      public void handleHttpIOException(IOException e) {
        delegate.handleError(e);
      }
    };
  }

  protected class TodoResource extends BaseResource {
    public TodoResource(URI uri, boolean rewrite) {
      super(uri, rewrite);
    }

    @Override
    protected void prepareClient() throws KeyManagementException, NoSuchAlgorithmException {
      super.prepareClient();
      context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }
  };

  protected void get(final String endpoint, final boolean wantBody,
      final TodoClientDelegate delegate) {
    URI uri;
    try {
      uri = new URI(serverURL + endpoint);
    } catch (URISyntaxException e) {
      delegate.handleError(e);
      return;
    }

    final TodoResource r = new TodoResource(uri, true);

    r.delegate = makeResourceDelegate(r, wantBody, delegate);

    r.get();
  }

  protected void post(final String endpoint, final HttpEntity body, final boolean wantBody,
      final TodoClientDelegate delegate) {
    URI uri;
    try {
      uri = new URI(serverURL + endpoint);
    } catch (URISyntaxException e) {
      delegate.handleError(e);
      return;
    }

    final TodoResource r = new TodoResource(uri, true);

    r.delegate = makeResourceDelegate(r, wantBody, delegate);

    r.post(body);
  }

  protected void post(final String endpoint, ExtendedJSONObject o, final boolean wantBody,
      final TodoClientDelegate delegate) {
    HttpEntity body;
    try {
      body = BaseResource.jsonEntity(o);
    } catch (UnsupportedEncodingException e) {
      delegate.handleError(e);
      return;
    }

    post(endpoint, body, wantBody, delegate);
  }

  public void login(final String assertion, final TodoClientDelegate delegate) {
    final ExtendedJSONObject o = new ExtendedJSONObject();
    o.put("assertion", assertion);
    o.put("audience", serverURL);

    cookieStore.clear();
    post("/api/verify", o, false, delegate);
  }

  public void status(final TodoClientDelegate delegate) {
    get("/api/auth_status", true, delegate);
  }

  public void logout(final TodoClientDelegate delegate) {
    final ExtendedJSONObject o = new ExtendedJSONObject();

    post("/api/logout", o, false, delegate);
  }

  public void get(final TodoClientDelegate delegate) {
    get("/api/todos/get", true, delegate);
  }

  public void save(final JSONArray todos, TodoClientDelegate delegate) {
    HttpEntity body;
    try {
      body = BaseResource.jsonEntity(todos);
    } catch (UnsupportedEncodingException e) {
      delegate.handleError(e);
      return;
    }

    post("/api/todos/save", body, false, delegate);
  }

  // For debugging.
  public String getSessionCookie() {
    final String SESSION_COOKIE_NAME = "123done";

    for (Cookie cookie : cookieStore.getCookies()) {
      if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }
}
