package org.mozilla.todo;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.mozilla.android.sync.test.helpers.MockResourceDelegate;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.ResourceDelegate;
import org.mozilla.gecko.sync.net.SyncResponse;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.CookieStore;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.client.protocol.ClientContext;
import ch.boye.httpclientandroidlib.cookie.Cookie;
import ch.boye.httpclientandroidlib.entity.StringEntity;
import ch.boye.httpclientandroidlib.impl.client.BasicCookieStore;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;

/**
 * Interface to 123done.org REST API for managing todo lists.
 * <p>
 * Initial login is authenticated by BrowserID assertion, and subsequent request
 * authentication is by secure session cookie.
 * <p>
 * This is a blocking interface for test code only.
 */
public class TodoServer {
  final CookieStore cookieStore = new BasicCookieStore();

  protected final String serverURL; // = "http://localhost:8080";
  protected final String host; // = "localhost:8080";

  public TodoServer(final String serverURL) {
    this.serverURL = serverURL;
    final String[] pieces = this.serverURL.split("://"); // This must be in URI, but I can't find it.
    this.host = pieces[pieces.length-1];
  }

  public TodoServer(final String serverURL, final String host) {
    this.serverURL = serverURL;
    this.host = host;
  }

  protected static StringEntity stringEntity(final String s) throws UnsupportedEncodingException {
    final StringEntity e = new StringEntity(s, "UTF-8");
    e.setContentType("application/json");
    return e;
  }

  /**
   * Helper for turning a JSON object into a payload.
   * @throws UnsupportedEncodingException
   */
  protected static StringEntity jsonEntity(final JSONObject body) throws UnsupportedEncodingException {
    return stringEntity(body.toJSONString());
  }

  protected static class Wrapper<T> {
    public T wrapped;
  };

  protected ResourceDelegate newResourceDelegate(final Wrapper<String> wrapper) {
    return new MockResourceDelegate() {
      @Override
      public void addHeaders(HttpRequestBase request, DefaultHttpClient client) {
        super.addHeaders(request, client);
        request.addHeader("Host", host);
      }

      @Override
      public void handleHttpResponse(final HttpResponse response) {
        final SyncResponse sr = new SyncResponse(response);

        handledHttpResponse = true;
        httpResponse = response;

        try {
          wrapper.wrapped = sr.body();
        } catch (Exception e) {
          waitHelper.performNotify(e);
          return;
        }

        int statusCode = response.getStatusLine().getStatusCode();
        BaseResource.consumeEntity(response);

        if (statusCode == 200) {
          waitHelper.performNotify();
        } else {
          waitHelper.performNotify(new RuntimeException("Expected response code 200 but got " + statusCode));
        }
      }
    };
  }

  protected ExtendedJSONObject blockingPost(final String uri, final ExtendedJSONObject body) throws URISyntaxException {
    return blockingPost(uri, body.toJSONString());
  }

  protected ExtendedJSONObject blockingPost(final String uri, final JSONArray body) throws URISyntaxException {
    return blockingPost(uri, body.toJSONString());
  }

  protected ExtendedJSONObject blockingPost(final String uri, final String body) throws URISyntaxException {
    final Wrapper<String> wrapper = new Wrapper<String>();

    final BaseResource r = new BaseResource(serverURL + uri, true) {
      @Override
      protected void prepareClient() throws KeyManagementException, NoSuchAlgorithmException {
        super.prepareClient();
        context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
      }
    };

    r.delegate = newResourceDelegate(wrapper);

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        try {
          r.post(stringEntity(body));
        } catch (UnsupportedEncodingException e) {
          WaitHelper.getTestWaiter().performNotify(e);
        }
      }
    });

    try {
      return ExtendedJSONObject.parseJSONObject(wrapper.wrapped);
    } catch (Exception e) {
      return null;
    }
  }

  protected String blockingGet(final String uri) throws URISyntaxException {
    final Wrapper<String> wrapper = new Wrapper<String>();

    final BaseResource r = new BaseResource(serverURL + uri, true) {
      @Override
      protected void prepareClient() throws KeyManagementException, NoSuchAlgorithmException {
        super.prepareClient();
        context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
      }
    };

    r.delegate = newResourceDelegate(wrapper);

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        r.get();
      }
    });

    return wrapper.wrapped;
  }

  public ExtendedJSONObject login(final String assertion) throws URISyntaxException {
    cookieStore.clear();

    final ExtendedJSONObject o = new ExtendedJSONObject();
    o.put("assertion", assertion);
    o.put("audience", serverURL);
    final ExtendedJSONObject verification = blockingPost("/api/verify", o);
    return verification;
  }

  public void logout() throws URISyntaxException {
    blockingPost("/api/logout", new ExtendedJSONObject());
  }

  public String getUserLoggedIn() {
    String body = null;
    try {
      body = blockingGet("/api/auth_status");
    } catch (Exception e) {
      throw new RuntimeException("Caught exception during GET /api/auth_status.", e);
    }

    if (body == null) {
      throw new RuntimeException("Null body during GET /api/auth_status.");
    }

    try {
      final ExtendedJSONObject in = ExtendedJSONObject.parseJSONObject(body);
      return (String) in.get("logged_in_email");
    } catch (Exception e) {
      throw new RuntimeException("Caught exception parsing /api/auth_status.", e);
    }
  }

  public JSONArray get() throws URISyntaxException {
    final String body = blockingGet("/api/todos/get");
    if (body == null) {
      throw new RuntimeException("Null body during GET /api/todos/get.");
    }

    final JSONArray todos = (JSONArray) JSONValue.parse(body);
    return todos;
  }

  public void save(final JSONArray todos) throws URISyntaxException {
    blockingPost("/api/todos/save", todos);
  }

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
