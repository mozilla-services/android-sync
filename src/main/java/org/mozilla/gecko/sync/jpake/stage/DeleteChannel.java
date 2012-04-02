package org.mozilla.gecko.sync.jpake.stage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.jpake.JPakeClient;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncResourceDelegate;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.message.BasicHeader;

public class DeleteChannel {
  private final String LOG_TAG = "DeleteChannel";

  public void execute(final JPakeClient jClient, final String reason) {
    BaseResource httpResource = null;
    try {
      httpResource = new BaseResource(jClient.channelUrl);
    } catch (URISyntaxException e) {
        Logger.info(LOG_TAG, "Encountered URISyntax exception, displaying abort anyways.");
        jClient.displayAbort(reason);
        return;
    }
    httpResource.delegate = new SyncResourceDelegate(httpResource) {

      @Override
      public void addHeaders(HttpRequestBase request, DefaultHttpClient client) {
        request.setHeader(new BasicHeader("X-KeyExchange-Id", jClient.clientId));
        request.setHeader(new BasicHeader("X-KeyExchange-Cid", jClient.channel));
      }

      @Override
      public void handleHttpResponse(HttpResponse response) {
        try {
          int statusCode = response.getStatusLine().getStatusCode();
          switch (statusCode) {
          case 200:
            Logger.info(LOG_TAG, "Successfully reported error to server.");
            break;
          case 403:
            Logger.info(LOG_TAG, "IP is blacklisted.");
            break;
          case 400:
            Logger.info(LOG_TAG, "Bad request (missing logs, or bad ids");
            break;
          default:
            Logger.info(LOG_TAG, "Server returned " + statusCode);
          }
        } finally {
          BaseResource.consumeEntity(response);
        }
        // Always call displayAbort, even if abort fails. We can't do anything about it.
        jClient.displayAbort(reason);
      }

      @Override
      public void handleHttpProtocolException(ClientProtocolException e) {
        Logger.info(LOG_TAG, "Encountered HttpProtocolException, displaying abort anyways.");
        jClient.displayAbort(reason);
      }

      @Override
      public void handleHttpIOException(IOException e) {
        Logger.info(LOG_TAG, "Encountered IOException, displaying abort anyways.");
        jClient.displayAbort(reason);
      }

      @Override
      public void handleTransportException(GeneralSecurityException e) {
        Logger.info(LOG_TAG, "Encountered GeneralSecurityException, displaying abort anyways.");
        jClient.displayAbort(reason);
      }
    };
    httpResource.delete();
  }

}
