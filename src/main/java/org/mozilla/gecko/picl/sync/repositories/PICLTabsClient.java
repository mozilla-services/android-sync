/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.picl.sync.repositories;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.ArrayList;

import org.mozilla.gecko.background.db.Tab;
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

public class PICLTabsClient {
  public final URI serverURI;
  public final String userid;

  public PICLTabsClient(URI serverURI, String userid) {
    this.serverURI = serverURI;
    this.userid = userid;
  }

  public interface PICLTabsDelegate {
    public void handleSuccess(ExtendedJSONObject extendedJSONObject);
    public void handleFailure(HttpResponse response, Exception e);
    public void handleError(Exception e);
  }

  protected class PICLTabsResourceDelegate extends BaseResourceDelegate {
    public final PICLTabsDelegate delegate;

    public PICLTabsResourceDelegate(Resource resource, PICLTabsDelegate delegate) {
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
      } else if (res.getStatusCode() == 304) {
        delegate.handleFailure(response, new RuntimeException("304 not yet handled"));
      } else {

        // TODO: remove this.  Just to help debug.
        try {
          System.out.println(res.body());
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
  }

  protected ResourceDelegate makeDelegate(BaseResource resource, PICLTabsDelegate delegate) {
    return new PICLTabsResourceDelegate(resource, delegate);
  }

  // GET <server-url>/<userid>/storage/<collection>
  public void getAllTabs(final PICLTabsDelegate delegate) {
    URI uri = URI.create(serverURI + "/" + userid + "/storage/tabs");

    BaseResource r = new BaseResource(uri);
    r.delegate = makeDelegate(r, delegate);

    r.get();
  }

  public void setTabs(ArrayList<Tab> tabs) {
    // Complete this.
  }
}
