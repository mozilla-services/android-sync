package org.mozilla.gecko.aitc.net;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mozilla.gecko.aitc.AppRecord;
import org.mozilla.gecko.aitc.DeviceRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;
import org.mozilla.gecko.sync.net.HMACAuthHeaderProvider;
import org.mozilla.gecko.sync.net.Resource;
import org.mozilla.gecko.sync.net.ResourceDelegate;
import org.mozilla.gecko.sync.net.SyncResponse;
import org.mozilla.gecko.tokenserver.TokenServerToken;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;

public class AppsClient {
  public static final String LOG_TAG = "AppsClient";

  protected final URI endpoint;
  protected final AuthHeaderProvider authHeaderProvider;

  public AppsClient(URI uri, AuthHeaderProvider authHeaderProvider) {
    this.endpoint = uri;
    this.authHeaderProvider = authHeaderProvider;
  }

  public AppsClient(TokenServerToken token) {
    this(URI.create(token.endpoint), new HMACAuthHeaderProvider(token.id, token.key));
  }

  public static class AppsClientException extends Exception {
    private static final long serialVersionUID = 7216021451027848587L;

    protected AppsClientException() {
      super();
    }

    public AppsClientException(String detailMessage) {
      super(detailMessage);
    }

    public static class AppsClientInvalidCredentialsException extends AppsClientException {
      private static final long serialVersionUID = 7216021451027848586L;
    }
  }

  public interface AppsClientDelegate {
    void handleSuccess();
    void handleRemoteFailure(AppsClientException e);
    void handleError(Exception e);
  }

  public interface AppsClientObjectDelegate extends AppsClientDelegate {
    void onObject(ExtendedJSONObject object);
  }

  public abstract class AppsClientResourceDelegate extends BaseResourceDelegate {
    protected final AppsClientDelegate delegate;

    public AppsClientResourceDelegate(Resource resource, AppsClientDelegate delegate) {
      super(resource);
      this.delegate = delegate;
    }

    @Override
    public void handleHttpProtocolException(ClientProtocolException e) {
      Logger.warn(LOG_TAG, "Got protocol exception.", e);
      delegate.handleError(e);
    }

    @Override
    public void handleHttpIOException(IOException e) {
      Logger.warn(LOG_TAG, "Got IO exception.", e);
      delegate.handleError(e);
    }

    @Override
    public void handleTransportException(GeneralSecurityException e) {
      Logger.warn(LOG_TAG, "Got protocol exception.", e);
      delegate.handleError(e);
    }

    @Override
    public AuthHeaderProvider getAuthHeaderProvider() {
      return authHeaderProvider;
    }
  }

  public void deleteApp(String appId, AppsClientDelegate delegate) {
    String uri = this.endpoint + "/apps/" + appId;

    deleteURI(uri, delegate);
  }

  public void deleteDevice(String uuid, AppsClientDelegate delegate) {
    String uri = this.endpoint + "/devices/" + uuid;

    deleteURI(uri, delegate);
  }

  protected void deleteURI(String uri, AppsClientDelegate delegate) {
    BaseResource r;
    try {
      r = new BaseResource(uri);
    } catch (URISyntaxException e) {
      delegate.handleError(e);
      return;
    }

    r.delegate = new AppsClientResourceDelegate(r, delegate) {
      @Override
      public void handleHttpResponse(HttpResponse response) {
        SyncResponse res = new SyncResponse(response);
        int statusCode = res.getStatusCode();

        Logger.debug(LOG_TAG, "Got response with status code " + statusCode + ".");

        String body = null;
        try {
          body = res.body();
        } catch (Exception e) {
          // Do nothing.
        }
        if (body != null) {
          Logger.pii(LOG_TAG, body);
        }

        if (statusCode == 204) {
          delegate.handleSuccess();
          return;
        }

        // XXX better later?
        delegate.handleRemoteFailure(new AppsClientException("Expected status code 204 but got " + statusCode));
      }
    };

    r.delete();
  }

  protected void putURI(String uri, JSONObject jsonObject, AppsClientDelegate delegate) {
    BaseResource r;
    try {
      r = new BaseResource(uri);
    } catch (URISyntaxException e) {
      delegate.handleError(e);
      return;
    }

    r.delegate = new AppsClientResourceDelegate(r, delegate) {
      @Override
      public void handleHttpResponse(HttpResponse response) {
        SyncResponse res = new SyncResponse(response);
        int statusCode = res.getStatusCode();

        Logger.debug(LOG_TAG, "Got response with status code " + statusCode + ".");

        String body = null;
        try {
          body = res.body();
        } catch (Exception e) {
          // Do nothing.
        }
        if (body != null) {
          Logger.pii(LOG_TAG, body);
        }

        if (statusCode == 201 || statusCode == 204) {
          delegate.handleSuccess();
          return;
        }

        // XXX better later?
        delegate.handleRemoteFailure(new AppsClientException("Expected status code 201 or 204 but got " + statusCode + "."));
      }
    };

    try {
      r.put(jsonObject);
    } catch (UnsupportedEncodingException e) {
      delegate.handleError(e);
    }
  }

  public void putApp(AppRecord app, AppsClientDelegate delegate) {
    String uri = this.endpoint + "/apps/" + app.appId();
    JSONObject jsonObject = app.toJSON().object;

    putURI(uri, jsonObject, delegate);
  }

  public void putDevice(DeviceRecord device, AppsClientDelegate delegate) {
    String uri = this.endpoint + "/devices/" + device.uuid;
    JSONObject jsonObject = device.toJSON().object;

    putURI(uri, jsonObject, delegate);
  }

  protected ResourceDelegate makeEachObjectDelegate(final String objectsKey,
      BaseResource resource, AppsClientObjectDelegate delegate) {
    return new AppsClientResourceDelegate(resource, delegate) {
      @Override
      public void handleHttpResponse(HttpResponse response) {
        SyncResponse res = new SyncResponse(response);
        int statusCode = res.getStatusCode();

        String body;
        try {
          body = res.body();
          Logger.debug(LOG_TAG, "Body: " + body);
        } catch (Exception e) {
          // Just debug code.
        }

        if (statusCode != 200) {
          delegate.handleRemoteFailure(new AppsClientException("Expected status code 200 but got " + statusCode + "."));
          return;
        }

        JSONArray objects;
        try {
          ExtendedJSONObject result = res.jsonObjectBody();
          objects = result.getArray(objectsKey);
        } catch (Exception e) {
          delegate.handleError(e);
          return;
        }

        for (Object object : objects) {
          if (!(object instanceof JSONObject)) {
            delegate.handleError(new AppsClientException("Malformed response: non-JSON object."));
            return;
          }

          ((AppsClientObjectDelegate) delegate).onObject(new ExtendedJSONObject((JSONObject) object));
        }

        delegate.handleSuccess();
      }
    };
  }

  protected ResourceDelegate makOneObjectDelegate(BaseResource resource, AppsClientObjectDelegate delegate) {
    return new AppsClientResourceDelegate(resource, delegate) {
      @Override
      public void handleHttpResponse(HttpResponse response) {
        SyncResponse res = new SyncResponse(response);
        int statusCode = res.getStatusCode();

        String body;
        try {
          body = res.body();
          Logger.debug(LOG_TAG, "Body: " + body);
        } catch (Exception e) {
          // Just debug code.
        }

        if (statusCode != 200) {
          delegate.handleRemoteFailure(new AppsClientException("Expected status code 200 but got " + statusCode + "."));
          return;
        }

        ExtendedJSONObject object;
        try {
          object = res.jsonObjectBody();
        } catch (Exception e) {
          delegate.handleError(e);
          return;
        }

        ((AppsClientObjectDelegate) delegate).onObject(object);

        delegate.handleSuccess();
      }
    };
  }

  protected URI makeGetURI(String path, long after, boolean full) throws URISyntaxException {
    StringBuilder uri = new StringBuilder();

    uri.append(this.endpoint.toString());
    uri.append(path);
    if (after > 0 || full) {
      uri.append("?");
      uri.append("after=");
      uri.append(after);
      uri.append("&");
      uri.append("full=");
      uri.append(full ? "1" : "0");
    }

    return new URI(uri.toString());
  }

  public void getApps(long after, boolean full, AppsClientObjectDelegate delegate) {
    URI uri;
    try {
      uri = makeGetURI("/apps/", after, full);
    } catch (URISyntaxException e) {
      delegate.handleError(e);
      return;
    }

    BaseResource r = new BaseResource(uri);

    r.delegate = makeEachObjectDelegate("apps", r, delegate);

    r.get();
  }

  public void getDevices(long after, boolean full, AppsClientObjectDelegate delegate) {
    URI uri;
    try {
      uri = makeGetURI("/devices/", after, full);
    } catch (URISyntaxException e) {
      delegate.handleError(e);
      return;
    }

    BaseResource r = new BaseResource(uri);

    r.delegate = makeEachObjectDelegate("devices", r, delegate);

    r.get();
  }

  public void getDevice(String uuid, AppsClientObjectDelegate delegate) {
    URI uri;
    try {
      uri = new URI(endpoint + "/devices/" + uuid);
    } catch (URISyntaxException e) {
      delegate.handleError(e);
      return;
    }

    BaseResource r = new BaseResource(uri);

    r.delegate = makOneObjectDelegate(r, delegate);

    r.get();
  }
}
