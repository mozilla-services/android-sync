package org.mozilla.gecko.sync.setup.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

import org.mozilla.gecko.sync.ThreadPool;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncResourceDelegate;
import org.mozilla.gecko.sync.net.SyncStorageRequest;
import org.mozilla.gecko.sync.setup.Constants;

import android.util.Base64;
import android.util.Log;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.message.BasicHeader;

public class AuthenticateAccountStage implements AuthenticatorStage {
  private final String LOG_TAG = "AuthenticateAccountStage";

  public interface AuthenticateAccountStageDelegate {
    public void handleSuccess(boolean isSuccess);
    public void handleFailure(HttpResponse response);
    public void handleError(Exception e);
  }

  @Override
  public void execute(final AccountAuthenticator aa) throws URISyntaxException, UnsupportedEncodingException {
    AuthenticateAccountStageDelegate callbackDelegate = new AuthenticateAccountStageDelegate() {

      @Override
      public void handleSuccess(boolean isSuccess) {
        aa.isSuccess = isSuccess;
        aa.runNextStage();
      }

      @Override
      public void handleFailure(HttpResponse response) {
        Log.d(LOG_TAG, "handleFailure");
        aa.abort(response.toString(), new Exception(response.getStatusLine().getStatusCode() + " error."));
        try {
          BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
          Log.w(LOG_TAG, "content: " + reader.readLine());
          SyncResourceDelegate.consumeReader(reader);
          reader.close();
          SyncResourceDelegate.consumeEntity(response.getEntity());
        } catch (IllegalStateException e) {
          Log.d(LOG_TAG, "Error reading content.", e);
        } catch (IOException e) {
          Log.d(LOG_TAG, "Error reading content.", e);
        }
      }

      @Override
      public void handleError(Exception e) {
        Log.d(LOG_TAG, "handleError");
        aa.abort("HTTP failure.", e);
      }
    };

    // Calculate BasicAuth hash of username/password.
    String authHash = Base64.encodeToString((aa.usernameHash + ":" + aa.password).getBytes(), Base64.DEFAULT);
    String authRequestUrl = aa.authServer + Constants.AUTH_SERVER_VERSION + aa.usernameHash + "/" + Constants.AUTH_SERVER_SUFFIX;
    authenticateAccount(callbackDelegate, authRequestUrl, authHash);
  }

  private void authenticateAccount(final AuthenticateAccountStageDelegate callbackDelegate, final String authRequestUrl, final String authHeader) throws URISyntaxException {
    final BaseResource httpResource = new BaseResource(authRequestUrl);
    httpResource.delegate = new SyncResourceDelegate(httpResource) {

      @Override
      public void addHeaders(HttpRequestBase request, DefaultHttpClient client) {

        client.log.enableDebug(true);
        request.setHeader(new BasicHeader("User-Agent", SyncStorageRequest.USER_AGENT));
        // Host header is not set for some reason, so do it explicitly.
        try {
          URI authServerUri = new URI(authRequestUrl);
          request.setHeader(new BasicHeader("Host", authServerUri.getHost()));
        } catch (URISyntaxException e) {
          Log.e(LOG_TAG, "Malformed uri, will be caught elsewhere.", e);
        }
        request.setHeader(new BasicHeader("Authorization", "Basic " + authHeader));
      }

      @Override
      public void handleHttpResponse(HttpResponse response) {
        int statusCode = response.getStatusLine().getStatusCode();
        switch(statusCode) {
        case 200:
          callbackDelegate.handleSuccess(true);
          SyncResourceDelegate.consumeEntity(response.getEntity());
          break;
        case 401:
          callbackDelegate.handleSuccess(false);
          SyncResourceDelegate.consumeEntity(response.getEntity());
          break;
        default:
          callbackDelegate.handleFailure(response);
        }
      }

      @Override
      public void handleHttpProtocolException(ClientProtocolException e) {
        Log.e(LOG_TAG, "Client protocol error.");
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
