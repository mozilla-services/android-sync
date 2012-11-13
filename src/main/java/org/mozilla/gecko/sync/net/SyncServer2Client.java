package org.mozilla.gecko.sync.net;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Logger;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;

public class SyncServer2Client {
  public static final String LOG_TAG = "SyncServer2Client";

  protected final URI uri;
  protected final AuthHeaderProvider authHeaderProvider;

  public SyncServer2Client(URI uri, AuthHeaderProvider authHeaderProvider) {
    this.uri = uri;
    this.authHeaderProvider = authHeaderProvider;
  }

  public interface SyncServer2InfoCollectionsDelegate {
    void onSuccess(ExtendedJSONObject result);
    void onRemoteFailure();
    void onLocalError(Exception e);
    void onRemoteError(Exception e);
  }

  public void getInfoCollections(final SyncServer2InfoCollectionsDelegate delegate) {
    String uri = this.uri + "/info/collections";

    getRecord(uri, delegate);
  }

  public void getMetaGlobal(final SyncServer2InfoCollectionsDelegate delegate) {
    String uri = this.uri + "/storage/meta/global";

    getRecord(uri, delegate);
  }

  protected void getRecord(String uri, final SyncServer2InfoCollectionsDelegate delegate) {
    BaseResource r;
    try {
      r = new BaseResource(uri);
    } catch (URISyntaxException e) {
      delegate.onLocalError(e);
      return;
    }

    r.delegate = new BaseResourceDelegate(r) {
      @Override
      public void handleHttpResponse(HttpResponse response) {
        SyncResponse res = new SyncResponse(response);
        int statusCode = res.getStatusCode();

        String body;
        try {
          body = res.body();
          Logger.debug(LOG_TAG, "Body: " + body);
        } catch (Exception e) {
          // XXX.
        }

        ExtendedJSONObject result;
        try {
          result = res.jsonObjectBody();
        } catch (Exception e) {
          delegate.onRemoteFailure();
          return;
        }

        if (statusCode != 200) {
          delegate.onRemoteFailure();
          return;
        }

        delegate.onSuccess(result);
      }

      @Override
      public void handleHttpProtocolException(ClientProtocolException e) {
        delegate.onLocalError(e);
      }

      @Override
      public void handleHttpIOException(IOException e) {
        delegate.onLocalError(e);
      }

      @Override
      public void handleTransportException(GeneralSecurityException e) {
        delegate.onRemoteError(e);
      }

      @Override
      public AuthHeaderProvider getAuthHeaderProvider() {
        return authHeaderProvider;
      }
    };

    r.get();
  }
}
