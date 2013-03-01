/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.bagheera;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;
import org.mozilla.gecko.sync.net.Resource;

import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.protocol.HTTP;

/**
 * Provides encapsulated access to a Bagheera document server.
 * The two permitted operations are:
 * * Delete a document.
 * * Upload a document, optionally deleting an expired document.
 */
public class BagheeraClient {

  protected final String serverURI;
  protected final Executor executor;
  protected static final Pattern URI_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

  protected static String PROTOCOL_VERSION = "1.0";
  protected static String SUBMIT_PATH = "/submit/";

  public BagheeraClient(final String serverURI, final Executor executor) {
    if (serverURI == null) {
      throw new IllegalArgumentException("Must provide a server URI.");
    }
    if (executor == null) {
      throw new IllegalArgumentException("Must provide a non-null executor.");
    }
    this.serverURI = serverURI.endsWith("/") ? serverURI : serverURI + "/";
    this.executor = executor;
  }

  public BagheeraClient(final String serverURI) {
    this(serverURI, Executors.newSingleThreadExecutor());
  }

  /**
   * Delete the specified document from the server.
   * The delegate's callbacks will be invoked by the BagheeraClient's executor.
   */
  public void deleteDocument(final String namespace,
                             final String id,
                             final BagheeraRequestDelegate delegate) throws URISyntaxException {
    if (namespace == null) {
      throw new IllegalArgumentException("Must provide namespace.");
    }
    if (id == null) {
      throw new IllegalArgumentException("Must provide id.");
    }

    final BaseResource resource = makeResource(namespace, id);
    resource.delegate = new BagheeraResourceDelegate(resource, delegate);
    resource.delete();
   }

  /**
   * Upload a JSON document to a Bagheera server.
   * The delegate's callbacks will be invoked by the BagheeraClient's executor.
   */
  public void uploadJSONDocument(final String namespace,
                                 final String id,
                                 final String payload,
                                 final String oldID,
                                 final BagheeraRequestDelegate delegate) throws URISyntaxException {
    if (namespace == null) {
      throw new IllegalArgumentException("Must provide namespace.");
    }
    if (id == null) {
      throw new IllegalArgumentException("Must provide id.");
    }
    if (payload == null) {
      throw new IllegalArgumentException("Must provide payload.");
    }

    final BaseResource resource = makeResource(namespace, id);
    final HttpEntity deflatedBody = DeflateHelper.deflateBody(payload);

    resource.delegate = new BagheeraUploadResourceDelegate(resource, oldID, delegate);
    resource.post(deflatedBody);
  }

  public static boolean isValidURIComponent(final String in) {
    return URI_PATTERN.matcher(in).matches();
  }

  protected BaseResource makeResource(final String namespace, final String id) throws URISyntaxException {
    if (!isValidURIComponent(namespace)) {
      throw new URISyntaxException(namespace, "Illegal namespace name. Must be alphanumeric + [_-].");
    }

    if (!isValidURIComponent(id)) {
      throw new URISyntaxException(id, "Illegal id value. Must be alphanumeric + [_-].");
    }

    final String uri = this.serverURI + PROTOCOL_VERSION + SUBMIT_PATH +
                       namespace + "/" + id;
    return new BaseResource(uri);
  }

  public class BagheeraResourceDelegate extends BaseResourceDelegate {
    private static final int DEFAULT_SOCKET_TIMEOUT_MSEC = 5 * 60 * 1000;       // Five minutes.
    protected BagheeraRequestDelegate delegate;

    public BagheeraResourceDelegate(Resource resource) {
      super(resource);
    }

    public BagheeraResourceDelegate(final Resource resource,
                                    final BagheeraRequestDelegate delegate) {
      this(resource);
      this.delegate = delegate;
    }

    @Override
    public int socketTimeout() {
      return DEFAULT_SOCKET_TIMEOUT_MSEC;
    }

    @Override
    public void handleHttpResponse(HttpResponse response) {
      final int status = response.getStatusLine().getStatusCode();
      switch (status) {
      case 200:
      case 201:
        invokeHandleSuccess(status, response);
        return;
      default:
        invokeHandleFailure(status, response);
      }
    }

    protected void invokeHandleError(final Exception e) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          delegate.handleError(e);
        }
      });
    }

    protected void invokeHandleFailure(final int status, final HttpResponse response) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          delegate.handleFailure(status, response);
        }
      });
    }

    protected void invokeHandleSuccess(final int status, final HttpResponse response) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          delegate.handleSuccess(status, response);
        }
      });
    }

    @Override
    public void handleHttpProtocolException(final ClientProtocolException e) {
      invokeHandleError(e);
    }

    @Override
    public void handleHttpIOException(IOException e) {
      invokeHandleError(e);
    }

    @Override
    public void handleTransportException(GeneralSecurityException e) {
      invokeHandleError(e);
    }
  }

  public final class BagheeraUploadResourceDelegate extends BagheeraResourceDelegate {
    private static final String HEADER_OBSOLETE_DOCUMENT = "X-Obsolete-Document";
    private static final String COMPRESSED_CONTENT_TYPE = "application/json+zlib; charset=utf-8";
    protected String obsoleteDocumentID;

    public BagheeraUploadResourceDelegate(Resource resource,
                                          String obsoleteDocumentID,
                                          BagheeraRequestDelegate delegate) {
      super(resource, delegate);
      this.obsoleteDocumentID = obsoleteDocumentID;
    }

    @Override
    public void addHeaders(HttpRequestBase request, DefaultHttpClient client) {
      super.addHeaders(request, client);
      request.setHeader(HTTP.CONTENT_TYPE, COMPRESSED_CONTENT_TYPE);
      if (this.obsoleteDocumentID != null) {
        request.addHeader(HEADER_OBSOLETE_DOCUMENT, this.obsoleteDocumentID);
      }
    }
  }
}
