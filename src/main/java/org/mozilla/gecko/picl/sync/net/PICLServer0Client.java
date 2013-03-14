package org.mozilla.gecko.picl.sync.net;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.GeneralSecurityException;

import org.json.simple.JSONArray;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;
import org.mozilla.gecko.sync.net.Resource;
import org.mozilla.gecko.sync.net.ResourceDelegate;
import org.mozilla.gecko.sync.net.SyncResponse;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;

public class PICLServer0Client {
  private static final String LOG_TAG = PICLServer0Client.class.getSimpleName();
  
  public final String serverURI;
  public final String userid;
  public final String collection;
  
  public PICLServer0Client(String serverURI, String userid, String collection) {
    this.serverURI = serverURI;
    this.userid = userid;
    this.collection = collection;
  }
  
  //GET <server-url>/<userid>/storage/<collection>
  public void get(PICLServer0ClientDelegate delegate) {
    get(uri(), delegate);    
  }
  
  /*public void get(String id, PICLServer0ClientDelegate delegate) {
    get(uri(id), delegate);
  }*/
  
  protected void get(URI uri, PICLServer0ClientDelegate delegate) {
    BaseResource r = new BaseResource(uri);
    r.delegate = makeDelegate(r, delegate);

    r.get();
  }
  
  public void post(JSONArray json, PICLServer0ClientDelegate delegate) {
    post(uri(), json, delegate);
  }
  
  /*public void post(String id, ExtendedJSONObject json, PICLServer0ClientDelegate delegate) {
    post(uri(id), json, delegate);
  }*/
  
  protected void post(URI uri, JSONArray json, PICLServer0ClientDelegate delegate) {
    BaseResource r = new BaseResource(uri);
    r.delegate = makeDelegate(r, delegate);

    try {
      r.post(json);
    } catch (UnsupportedEncodingException e) {
      delegate.handleError(e);
    }
  }
  
  protected URI uri() {
    return uri("");
  }
  
  protected URI uri(String id) {
    return URI.create(serverURI + "/" + userid + "/storage/" + collection + id);
  }
  
  
  public interface PICLServer0ClientDelegate {
    public void handleSuccess(ExtendedJSONObject json);
    public void handleFailure(HttpResponse response, Exception e);
    public void handleError(Exception e);
  }
  
  protected class PICLServer0ResourceDelegate extends BaseResourceDelegate {
    public final PICLServer0ClientDelegate delegate;

    public PICLServer0ResourceDelegate(Resource resource, PICLServer0ClientDelegate delegate) {
      super(resource);
      this.delegate = delegate;
    }

    @Override
    public void addHeaders(HttpRequestBase request, DefaultHttpClient client) {
      request.addHeader("Authorization", userid);
    }

    @Override
    public void handleHttpResponse(HttpResponse response) {
      SyncResponse res = new SyncResponse(response);
      if (res.getStatusCode() == 200) {
        try {
          delegate.handleSuccess(res.jsonObjectBody());
        } catch (Exception e) {
          delegate.handleFailure(response, e);
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

        delegate.handleFailure(response, new RuntimeException("Bad response code " + res.getStatusCode()));
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

  protected ResourceDelegate makeDelegate(BaseResource resource, PICLServer0ClientDelegate delegate) {
    return new PICLServer0ResourceDelegate(resource, delegate);
  }
}
