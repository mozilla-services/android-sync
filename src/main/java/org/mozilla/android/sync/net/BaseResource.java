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
import java.net.URI;
import java.net.URISyntaxException;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.auth.Credentials;
import ch.boye.httpclientandroidlib.auth.UsernamePasswordCredentials;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.methods.HttpDelete;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.client.methods.HttpPut;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.client.methods.HttpUriRequest;
import ch.boye.httpclientandroidlib.conn.ClientConnectionManager;
import ch.boye.httpclientandroidlib.impl.auth.BasicScheme;
import ch.boye.httpclientandroidlib.impl.client.AbstractHttpClient;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.impl.conn.SingleClientConnManager;
import ch.boye.httpclientandroidlib.params.HttpConnectionParams;
import ch.boye.httpclientandroidlib.params.HttpParams;
import ch.boye.httpclientandroidlib.protocol.BasicHttpContext;
import ch.boye.httpclientandroidlib.protocol.HttpContext;

/**
 * Provide simple HTTP access to a Sync server or similar.
 * Implements Basic Auth by asking its delegate for credentials.
 * Communicates with a ResourceDelegate to asynchronously return responses and errors.
 * Exposes simple get/post/put/delete methods.
 */
public class BaseResource implements Resource {
  protected URI uri;
  protected BasicHttpContext context;
  protected DefaultHttpClient client;
  public    ResourceDelegate delegate;
  protected HttpRequestBase request;

  public BaseResource(String uri) throws URISyntaxException {
	  this(new URI(uri));
  }
  public BaseResource(URI uri) {
    this.uri = uri;
  }

  /**
   * Apply the provided credentials string to the provided request.
   * @param credentials
   *        A string, "user:pass".
   * @param client
   * @param request
   * @param context
   */
  private static void applyCredentials(String credentials, AbstractHttpClient client, HttpUriRequest request, HttpContext context) {
    Credentials creds = new UsernamePasswordCredentials(credentials);
    Header header = BasicScheme.authenticate(creds, "US-ASCII", false);
    request.addHeader(header);
    System.out.println("Adding auth header " + header);
  }

  /**
   * Invoke this after delegate and request have been set.
   */
  private void prepareClient() {
    context = new BasicHttpContext();
    client = new DefaultHttpClient(this.getConnectionManager());

    // TODO: Eventually we should use Apache HttpAsyncClient. It's not out of alpha yet.
    // Until then, we synchronously make the request, then invoke our delegate's callback.
    String credentials = delegate.getCredentials();
    if (credentials != null) {
      BaseResource.applyCredentials(credentials, client, request, context);
    }

    HttpParams params = client.getParams();
    HttpConnectionParams.setConnectionTimeout(params, delegate.connectionTimeout());
    HttpConnectionParams.setSoTimeout(params, delegate.socketTimeout());
    delegate.addHeaders(request, client);
  }

  private ClientConnectionManager getConnectionManager() {
    // TODO: memoize; scheme registry; shutdown.
    return new SingleClientConnManager();
  }

  private void execute() {
    try {
      HttpResponse response = client.execute(request, context);
      delegate.handleHttpResponse(response);
    } catch (ClientProtocolException e) {
      delegate.handleHttpProtocolException(e);
    } catch (IOException e) {
      delegate.handleHttpIOException(e);
    }
  }

  private void go(HttpRequestBase request) {
   if (delegate == null) {
      throw new IllegalArgumentException("No delegate provided.");
    }
    this.request = request;
    this.prepareClient();
    this.execute();
  }

  @Override
  public void get() {
    this.go(new HttpGet(this.uri));
  }

  @Override
  public void delete() {
    this.go(new HttpDelete(this.uri));
  }

  @Override
  public void post(HttpEntity body) {
    HttpPost request = new HttpPost(this.uri);
    request.setEntity(body);
    this.go(request);
  }

  @Override
  public void put(HttpEntity body) {
    HttpPut request = new HttpPut(this.uri);
    request.setEntity(body);
    this.go(request);
  }
}
