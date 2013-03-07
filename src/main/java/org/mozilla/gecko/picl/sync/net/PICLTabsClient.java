/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

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
import org.mozilla.gecko.sync.repositories.domain.TabsRecord;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;

public class PICLTabsClient {
  private static final String LOG_TAG = PICLTabsClient.class.getSimpleName();

  public final String serverURI;
  public final String userid;

  public PICLTabsClient(String serverURI, String userid) {
    this.serverURI = serverURI;
    this.userid = userid;
  }

  public interface PICLTabsDelegate {
    public void handleSuccess(ExtendedJSONObject json);
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

  @SuppressWarnings("unchecked")
  public void setTabs(TabsRecord tabsRecord, PICLTabsDelegate delegate) {
    URI uri = URI.create(serverURI + "/" + userid + "/storage/tabs");

    ExtendedJSONObject json = new ExtendedJSONObject();
    json.put("id", tabsRecord.guid);

    ExtendedJSONObject payload = new ExtendedJSONObject();
    tabsRecord.populatePayload(payload);

    json.put("payload", payload.toJSONString());

    JSONArray jsonArr = new JSONArray();
    jsonArr.add(json.object);

    BaseResource r = new BaseResource(uri);
    r.delegate = makeDelegate(r, delegate);

    try {
      Logger.warn(LOG_TAG, "POST: " + jsonArr.toString());
      r.post(jsonArr);
    } catch (UnsupportedEncodingException e) {
      delegate.handleError(e);
    }
  }
}
