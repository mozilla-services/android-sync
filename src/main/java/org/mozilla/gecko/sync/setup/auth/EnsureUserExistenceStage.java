package org.mozilla.gecko.sync.setup.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncResourceDelegate;
import org.mozilla.gecko.sync.setup.Constants;

import android.util.Log;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;

public class EnsureUserExistenceStage implements AuthenticatorStage {
  private final String LOG_TAG = "EnsureUserExistenceStage";

  public interface EnsureUserExistenceStageDelegate {
    public void handleSuccess();
    public void handleFailure();
    public void handleError(Exception e);
  }
  @Override
  public void execute(final AccountAuthenticator aa) throws URISyntaxException,
      UnsupportedEncodingException {
    final EnsureUserExistenceStageDelegate callbackDelegate = new EnsureUserExistenceStageDelegate() {

      @Override
      public void handleSuccess() {
        // User exists; now determine auth node.
        aa.runNextStage();
      }

      @Override
      public void handleFailure() {
        aa.abort("No such user,", new Exception("No user."));
      }

      @Override
      public void handleError(Exception e) {
        aa.abort("Error checking user existence.", e);
      }

    };

    String userRequestUrl = aa.nodeServer + Constants.AUTH_NODE_PATHNAME + Constants.AUTH_NODE_VERSION + aa.usernameHash;
    final BaseResource httpResource = new BaseResource(userRequestUrl);
    httpResource.delegate = new SyncResourceDelegate(httpResource) {

      @Override
      public void handleHttpResponse(HttpResponse response) {
        int statusCode = response.getStatusLine().getStatusCode();
        switch(statusCode) {
        case 200:
          try {
            InputStream content = response.getEntity().getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(content, "UTF-8"), 1024);
            String inUse = reader.readLine();
            Log.d(LOG_TAG, "username inUse:" + inUse);
            if (inUse.equals("1")) { // Username exists.
              callbackDelegate.handleSuccess();
            } else { // User does not exist.
              callbackDelegate.handleFailure();
            }
            SyncResourceDelegate.consumeReader(reader);
            reader.close();
          } catch (IllegalStateException e) {
            callbackDelegate.handleError(e);
          } catch (IOException e) {
            callbackDelegate.handleError(e);
          }
          break;
        default: // No other response is acceptable.
          callbackDelegate.handleFailure();
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
    AccountAuthenticator.runOnThread(new Runnable() {

      @Override
      public void run() {
        httpResource.get();
      }
    });
  }

}
