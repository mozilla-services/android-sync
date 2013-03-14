package org.mozilla.gecko.picl.account.net;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.GeneralSecurityException;

import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;
import org.mozilla.gecko.sync.net.Resource;
import org.mozilla.gecko.sync.net.ResourceDelegate;
import org.mozilla.gecko.sync.net.SyncResponse;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.entity.StringEntity;

public class PICLKeyServer0Client {

  private static final String LOG_TAG = "PICLKeyServer0Client";

  private static final String USER_ROUTE = "user";

  public String serverURL;
  protected final KeyResponse key = new KeyResponse();

  public PICLKeyServer0Client(String url) {
    serverURL = url;
  }

  public void get(String email, PICLKeyServer0ClientDelegate delegate) {
    key.email = email;

    BaseResource r = new BaseResource(uri(email));
    r.delegate = makeGetDelegate(r, email, delegate);

    r.get();
  }


  protected void post(String email, PICLKeyServer0ClientDelegate delegate) {
    BaseResource r = new BaseResource(uri(null));
    r.delegate = makeDelegate(r, delegate);

    StringEntity entity;
    try {
      entity = new StringEntity("email=" + email);
      entity.setContentType("application/x-www-form-urlencoded");
    } catch (UnsupportedEncodingException e) {
      delegate.handleError(e);
      return;
    }
    r.post(entity);
  }

  protected URI uri(String email) {
    return URI.create(serverURL + "/" + USER_ROUTE + (email != null ? "?email=" + email : ""));
  }

  protected ResourceDelegate makeDelegate(BaseResource resource, PICLKeyServer0ClientDelegate delegate) {
    return new PICLKeyServer0ResourceDelegate(resource, delegate);
  }

  protected ResourceDelegate makeGetDelegate(BaseResource resource, final String email, final PICLKeyServer0ClientDelegate delegate) {
    return new PICLKeyServer0ResourceDelegate(resource, new PICLKeyServer0ClientDelegate() {

      @Override
      public void handleKey(KeyResponse key) {
        delegate.handleKey(key);
      }

      @Override
      public void handleError(Exception e) {
        Logger.warn(LOG_TAG, "Error getting key: " + e);
        post(email, delegate);
      }

    });
  }

  protected class PICLKeyServer0ResourceDelegate extends BaseResourceDelegate {
    public final String LOG_TAG = PICLKeyServer0ResourceDelegate.class.getSimpleName();
    public final PICLKeyServer0ClientDelegate delegate;

    public PICLKeyServer0ResourceDelegate(Resource resource, PICLKeyServer0ClientDelegate delegate) {
      super(resource);
      this.delegate = delegate;
    }

    @Override
    public void handleHttpResponse(HttpResponse response) {
      SyncResponse res = new SyncResponse(response);
      int code = res.getStatusCode();
      if (code >= 200 && code < 300) {
        try {
          ExtendedJSONObject json = res.jsonObjectBody();

          key.kA = json.getString("kA");
          key.deviceId = json.getString("deviceId");
          key.version = String.valueOf(json.getLong("version"));

          delegate.handleKey(key);
        } catch (Exception e) {
          delegate.handleError(e);
        }
      } else {

        //TODO: Remove this. Just to help debug.
        try {
          Logger.warn(LOG_TAG, res.body());
        } catch (IllegalStateException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        delegate.handleError(new RuntimeException("Bad response code " + res.getStatusCode()));
      }
    }

    @Override
    public void handleHttpProtocolException(ClientProtocolException e) {
      delegate.handleError(e);
    }

    @Override
    public void handleHttpIOException(IOException e) {
      delegate.handleError(e);
    }

    @Override
    public void handleTransportException(GeneralSecurityException e) {
      delegate.handleError(e);
    }
  }

  public static interface PICLKeyServer0ClientDelegate {
    public void handleKey(KeyResponse key);
    public void handleError(Exception e);
  }

  public static class KeyResponse {
    public String email;
    public String kA;
    public String version;
    public String deviceId;
  }

}
