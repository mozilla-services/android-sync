/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.ThreadPool;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncResourceDelegate;

import android.util.Log;
import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;

public class EnsureClusterURLStage implements GlobalSyncStage {
  public interface ClusterURLFetchDelegate {
    public void handleSuccess(String url);
    public void handleFailure(HttpResponse response);
    public void handleError(Exception e);
  }

  protected static final String LOG_TAG = "EnsureClusterURLStage";

  // TODO: if cluster URL has changed since last time, we need to ensure that we do
  // a fresh start. This takes place at the GlobalSession level. Verify!
  public static void fetchClusterURL(final GlobalSession session,
                                     final ClusterURLFetchDelegate delegate) throws URISyntaxException {
    Log.i(LOG_TAG, "In fetchClusterURL. Server URL is " + session.config.serverURL);
    String nodeWeaveURL = session.config.nodeWeaveURL();
    Log.d(LOG_TAG, "node/weave is " + nodeWeaveURL);

    BaseResource resource = new BaseResource(nodeWeaveURL);
    resource.delegate = new SyncResourceDelegate(resource) {

      @Override
      public void handleHttpResponse(HttpResponse response) {
        int status = response.getStatusLine().getStatusCode();
        switch (status) {
        case 200:
          Log.i(LOG_TAG, "Got 200 for node/weave fetch.");
          // Great!
          HttpEntity entity = response.getEntity();
          if (entity == null) {
            delegate.handleSuccess(null);
            return;
          }
          String output = null;
          try {
            InputStream content = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(content, "UTF-8"), 1024);
            output = reader.readLine();
            SyncResourceDelegate.consumeReader(reader);
            reader.close();
          } catch (IllegalStateException e) {
            delegate.handleError(e);
          } catch (IOException e) {
            delegate.handleError(e);
          }

          if (output == null || output.equals("null")) {
            delegate.handleSuccess(null);
          }
          delegate.handleSuccess(output);
          break;
        case 400:
        case 404:
          Log.i(LOG_TAG, "Got " + status + " for cluster URL request.");
          delegate.handleFailure(response);
          SyncResourceDelegate.consumeEntity(response.getEntity());
          break;
        default:
          Log.w(LOG_TAG, "Got " + status + " fetching node/weave. Returning failure.");
          delegate.handleFailure(response);
          SyncResourceDelegate.consumeEntity(response.getEntity());
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
    };

    resource.get();
  }

  public void execute(final GlobalSession session) throws NoSuchStageException {

    if (session.config.getClusterURL() != null) {
      Log.i(LOG_TAG, "Cluster URL already set. Continuing with sync.");
      session.advance();
      return;
    }

    Log.i(LOG_TAG, "Fetching cluster URL.");
    final ClusterURLFetchDelegate delegate = new ClusterURLFetchDelegate() {

      @Override
      public void handleSuccess(final String url) {
        Log.i(LOG_TAG, "Node assignment pointed us to " + url);

        try {
          session.config.setClusterURL(url);
          ThreadPool.run(new Runnable() {
            @Override
            public void run() {
              session.advance();
            }
          });
          return;
        } catch (URISyntaxException e) {
          final URISyntaxException uriException = e;
          ThreadPool.run(new Runnable() {
            @Override
            public void run() {
              session.abort(uriException, "Invalid cluster URL.");
            }
          });
        }
      }

      @Override
      public void handleFailure(HttpResponse response) {
        int statusCode = response.getStatusLine().getStatusCode();
        Log.w(LOG_TAG, "Got HTTP failure fetching node assignment: " + statusCode);
        if (statusCode == 404) {
          URI serverURL = session.config.serverURL;
          if (serverURL != null) {
            Log.i(LOG_TAG, "Using serverURL <" + serverURL.toASCIIString() + "> as clusterURL.");
            session.config.setClusterURL(serverURL);
            session.advance();
            return;
          }
          Log.w(LOG_TAG, "No serverURL set to use as fallback cluster URL. Aborting sync.");
          // Fallthrough to abort.
        } else {
          session.interpretHTTPFailure(response);
        }
        session.abort(new Exception("HTTP failure."), "Got failure fetching cluster URL.");
      }

      @Override
      public void handleError(Exception e) {
        session.abort(e, "Got exception fetching cluster URL.");
      }
    };

    ThreadPool.run(new Runnable() {
      @Override
      public void run() {
        try {
          fetchClusterURL(session, delegate);
        } catch (URISyntaxException e) {
          session.abort(e, "Invalid URL for node/weave.");
        }
      }});
  }
}
