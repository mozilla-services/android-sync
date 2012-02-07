package org.mozilla.gecko.sync.setup.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

import org.mozilla.gecko.sync.ThreadPool;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncResourceDelegate;
import org.mozilla.gecko.sync.setup.Constants;

import android.util.Log;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;

public class FetchUserNodeStage implements AuthenticatorStage {
  private final String LOG_TAG = "FetchUserNodeStage";

  public interface FetchNodeStageDelegate {
    public void handleSuccess(String url);
    public void handleFailure(HttpResponse response);
    public void handleError(Exception e);
  }

  @Override
  public void execute(final AccountAuthenticator aa) throws URISyntaxException {

    FetchNodeStageDelegate callbackDelegate = new FetchNodeStageDelegate() {

      @Override
      public void handleSuccess(String server) {
        if (!server.endsWith("/")) {
          server += "/";
        }
        aa.authServer = server;
        aa.runNextStage();
      }

      @Override
      public void handleFailure(HttpResponse response) {
        if (response.getStatusLine().getStatusCode() == 404) {
          aa.abort(response.toString(), new Exception(LOG_TAG + " 404 no such user."));
        } else {
          aa.abort(response.toString(), new Exception(response.getStatusLine().getStatusCode() + " error."));
        }
      }

      @Override
      public void handleError(Exception e) {
        aa.abort("HTTP failure.", e);
      }
    };
    String nodeRequestUrl = aa.nodeServer + Constants.AUTH_NODE_PATHNAME + Constants.AUTH_NODE_VERSION + aa.usernameHash + "/" + Constants.AUTH_NODE_SUFFIX;
    Log.d(LOG_TAG, "nodeUrl: " + nodeRequestUrl);
    makeFetchNodeRequest(callbackDelegate, nodeRequestUrl);
  }

  private void makeFetchNodeRequest(final FetchNodeStageDelegate callbackDelegate, String fetchNodeUrl) throws URISyntaxException {
    // Fetch node containing user.
    final BaseResource httpResource = new BaseResource(fetchNodeUrl);
    httpResource.delegate = new SyncResourceDelegate(httpResource) {

      @Override
      public void handleHttpResponse(HttpResponse response) {
        int statusCode = response.getStatusLine().getStatusCode();
        switch(statusCode) {
        case 200:
          try {
            InputStream content = response.getEntity().getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(content, "UTF-8"), 1024);
            String server = reader.readLine();
            callbackDelegate.handleSuccess(server);
            SyncResourceDelegate.consumeReader(reader);
            reader.close();
          } catch (IllegalStateException e) {
            callbackDelegate.handleError(e);
          } catch (IOException e) {
            callbackDelegate.handleError(e);
          }
          break;
        default:
          // No other acceptable states.
          callbackDelegate.handleFailure(response);
        }
        SyncResourceDelegate.consumeEntity(response.getEntity());
      }

      @Override
      public void handleHttpProtocolException(ClientProtocolException e) {
        callbackDelegate.handleError(e);
      }

      @Override
      public void handleHttpIOException(IOException e) {
        callbackDelegate.handleError(e);
      }

      @Override
      public void handleTransportException(GeneralSecurityException e) {
        callbackDelegate.handleError(e);
      }

    };
    runOnThread(new Runnable() {

      @Override
      public void run() {
        httpResource.get();
      }
    });
  }

  private static void runOnThread(Runnable run) {
    ThreadPool.run(run);
  }
}