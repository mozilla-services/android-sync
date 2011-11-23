package org.mozilla.android.sync.net;

import java.io.IOException;

import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.params.CoreProtocolPNames;

public class SyncStorageRequest implements Resource {

  /**
   * A ResourceDelegate that mediates between Resource-level notifications and the SyncStorageRequest.
   */
  public class SyncResourceDelegate implements ResourceDelegate {

    protected String contentType = "text/plain";
    private SyncStorageRequest request;

    SyncResourceDelegate(SyncStorageRequest request) {
      this.request = request;
    }

    @Override
    public String getCredentials() {
      return this.request.delegate.credentials();
    }

    @Override
    public void handleResponse(HttpResponse response) {
      SyncStorageRequestDelegate d = this.request.delegate;
      SyncStorageResponse res = new SyncStorageResponse(response);
      if (res.wasSuccessful()) {
        d.handleSuccess(res);
      } else {
        d.handleFailure(res);
      }
    }

    @Override
    public void handleProtocolException(ClientProtocolException e) {
      this.request.delegate.handleError(e);
    }

    @Override
    public void handleIOException(IOException e) {
      this.request.delegate.handleError(e);
    }

    @Override
    public int connectionTimeout() {
      return 30 * 1000;             // Wait 30s for a connection to open.
    }
    @Override
    public int socketTimeout() {
      return 5 * 60 * 1000;         // Wait 5 minutes for data.
    }

    @Override
    public void addHeaders(HttpRequestBase request, DefaultHttpClient client) {
      client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, USER_AGENT);

      // Clients can use their delegate interface to specify X-Weave-If-Unmodified-Since.
      String ifUnmodifiedSince = this.request.delegate.ifUnmodifiedSince();
      if (ifUnmodifiedSince != null) {
        request.setHeader("X-Weave-If-Unmodified-Since", ifUnmodifiedSince);
      }
    }
  }

  public static String USER_AGENT = "Firefox AndroidSync 0.1";
  protected SyncResourceDelegate resourceDelegate;
  public SyncStorageRequestDelegate delegate;
  protected BaseResource resource;

  public SyncStorageRequest() {
    super();
  }

  // Default implementation. Override this.
  protected SyncResourceDelegate makeResourceDelegate(SyncStorageRequest request) {
    return new SyncResourceDelegate(request);
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

  public void put(HttpEntity body) {
    this.resource.put(body);
  }
}