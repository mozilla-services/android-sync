/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Android Sync Client.
 *
 * The Initial Developer of the Original Code is
 * the Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *  Richard Newman <rnewman@mozilla.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.android.sync.net;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.json.simple.JSONObject;

/**
 * Resource class that implements expected headers and processing for Sync.
 * Accepts a simplified delegate.
 *
 * Includes:
 * * Basic Auth headers (via Resource)
 * * Error responses:
 *   * 401
 *   * 503
 * * Headers:
 *   * Retry-After
 *   * X-Weave-Backoff
 *   * X-Weave-Records?
 *   * ...
 * * Timeouts
 * * Network errors
 * * application/newlines
 * * JSON parsing
 * * Content-Type and Content-Length validation.
 */
public class SyncStorageRequest implements Resource {
  public static String USER_AGENT = "Firefox AndroidSync 0.1";

  // The delegate that receives callbacks from `resource`…
  private SyncResourceDelegate resourceDelegate;

  // … and the delegate we in turn notify.
  public SyncStorageRequestDelegate delegate;

  // The resource that's actually performing the request.
  protected BaseResource resource;

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

  /**
   * @param uri
   * @throws URISyntaxException
   */
  public SyncStorageRequest(String uri) throws URISyntaxException {
    this(new URI(uri));
  }

  /**
   * @param uri
   */
  public SyncStorageRequest(URI uri) {
    this.resource = new BaseResource(uri);
    this.resourceDelegate = new SyncResourceDelegate(this);
    this.resource.delegate = this.resourceDelegate;
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

  /**
   * Helper for turning a JSON object into a payload.
   * @param body
   * @return
   * @throws UnsupportedEncodingException
   */
  private StringEntity jsonEntity(JSONObject body) throws UnsupportedEncodingException {
    StringEntity e = new StringEntity(body.toJSONString(), "UTF-8");
    e.setContentType("application/json");
    return e;
  }

  public void post(JSONObject body) {
    // Let's do this the trivial way for now.
    try {
      this.resource.post(jsonEntity(body));
    } catch (UnsupportedEncodingException e) {
      this.delegate.handleError(e);
    }
  }

  public void put(JSONObject body) {
    // Let's do this the trivial way for now.
    try {
      this.resource.put(jsonEntity(body));
    } catch (UnsupportedEncodingException e) {
      this.delegate.handleError(e);
    }
  }
}
