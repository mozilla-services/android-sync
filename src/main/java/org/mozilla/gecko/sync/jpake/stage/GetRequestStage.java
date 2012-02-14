/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.jpake.stage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Timer;
import java.util.TimerTask;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.jpake.JPakeClient;
import org.mozilla.gecko.sync.jpake.JPakeResponse;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.Resource;
import org.mozilla.gecko.sync.net.SyncResourceDelegate;
import org.mozilla.gecko.sync.setup.Constants;

import android.util.Log;
import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.message.BasicHeader;

public class GetRequestStage implements JPakeStage {
  private final static String LOG_TAG = "GetRequestStage";
  private Timer timerScheduler = new Timer();
  private int pollTries;
  private GetStepTimerTask getStepTimerTask;

  private interface GetRequestStageDelegate {
    public void handleSuccess(HttpResponse response);
    public void handleFailure(String error);
    public void handleError(Exception e);
  }

  @Override
  public void execute(final JPakeClient jClient) {
    Log.d(LOG_TAG, "Retrieving next message.");

    final GetRequestStageDelegate callbackDelegate = new GetRequestStageDelegate() {

      @Override
      public void handleSuccess(HttpResponse response) {
        if (jClient.finished) {
          Log.d(LOG_TAG, "Finished; returning.");
          return;
        }
        JPakeResponse res = new JPakeResponse(response);

        Header[] etagHeaders = response.getHeaders("etag");
        if (etagHeaders == null) {
          Log.e(LOG_TAG, "Server did not supply ETag.");
          jClient.abort(Constants.JPAKE_ERROR_SERVER);
          return;
        }

        jClient.theirEtag = etagHeaders[0].toString();
        Log.i(LOG_TAG, "their Etag: " + jClient.theirEtag);
        try {
          jClient.jIncoming = res.jsonObjectBody();
        } catch (IllegalStateException e) {
          Log.e(LOG_TAG, "Illegal state.", e);
          jClient.abort(Constants.JPAKE_ERROR_INVALID);
          return;
        } catch (IOException e) {
          Log.e(LOG_TAG, "I/O Exception.", e);
          jClient.abort(Constants.JPAKE_ERROR_INVALID);
          return;
        } catch (ParseException e) {
          Log.e(LOG_TAG, "Parse Exception.", e);
          jClient.abort(Constants.JPAKE_ERROR_INVALID);
          return;
        } catch (NonObjectJSONException e) {
          Log.e(LOG_TAG, "JSON exception.", e);
          jClient.abort(Constants.JPAKE_ERROR_INVALID);
          return;
        }
        Log.d(LOG_TAG, "incoming message: " + jClient.jIncoming.toJSONString());

        jClient.runNextStage();
      }

      @Override
      public void handleFailure(String error) {
        jClient.abort(error);
      }

      @Override
      public void handleError(Exception e) {
        Log.e(LOG_TAG, "Threw HTTP exception.", e);
        jClient.abort(Constants.JPAKE_ERROR_NETWORK);
      }
    };

    Resource httpRequest;
    try {
      httpRequest = createGetRequest(callbackDelegate, jClient);
    } catch (URISyntaxException e) {
      Log.e(LOG_TAG, "Incorrect URI syntax.", e);
      jClient.abort(Constants.JPAKE_ERROR_INVALID);
      return;
    }

    Log.d(LOG_TAG, "Scheduling GET request.");
    getStepTimerTask = new GetStepTimerTask(httpRequest);
    timerScheduler.schedule(getStepTimerTask, jClient.jpakePollInterval);
  }

  private Resource createGetRequest(final GetRequestStageDelegate callbackDelegate, final JPakeClient jpakeClient) throws URISyntaxException {
    BaseResource httpResource = new BaseResource(jpakeClient.channelUrl);
    httpResource.delegate = new SyncResourceDelegate(httpResource) {

      @Override
      public void addHeaders(HttpRequestBase request, DefaultHttpClient client) {
        Log.d(LOG_TAG, "making poll request " + jpakeClient.pollTries);
        request.setHeader(new BasicHeader("X-KeyExchange-Id", jpakeClient.clientId));
        if (jpakeClient.myEtag != null) {
          Log.d(LOG_TAG, "Setting 'If-None-Match' " + jpakeClient.myEtag);
          request.setHeader(new BasicHeader("If-None-Match", jpakeClient.myEtag));
        }
      }

      @Override
      public void handleHttpResponse(HttpResponse response) {
        Header[] etagHeaders = response.getHeaders("etag");
        if (etagHeaders == null || etagHeaders.length < 1) {
          Log.e(LOG_TAG, "Server did not supply ETag.");
        } else {
          Log.d(LOG_TAG, "Sender header: " + etagHeaders[0]);
        }
        int statusCode = response.getStatusLine().getStatusCode();
        switch (statusCode) {
        case 200:
          Log.d(LOG_TAG, "GET 200.");
          jpakeClient.pollTries = 0; // Reset pollTries for next GET.
          callbackDelegate.handleSuccess(response);
          break;
        case 304:
          Log.d(LOG_TAG, "Channel hasn't been updated yet. Will try again later");
          if (pollTries >= jpakeClient.jpakeMaxTries) {
            Log.e(LOG_TAG, "Tried for " + pollTries + " times, maxTries " + jpakeClient.jpakeMaxTries + ", aborting");
            callbackDelegate.handleFailure(Constants.JPAKE_ERROR_TIMEOUT);
            break;
          }
          jpakeClient.pollTries += 1;
          if (!jpakeClient.finished) {
            Log.d(LOG_TAG, "Scheduling next GET request.");
            scheduleGetRequest(jpakeClient.jpakePollInterval, jpakeClient);
          } else {
            Log.d(LOG_TAG, "Resetting pollTries");
            jpakeClient.pollTries = 0;
          }
          break;
        case 404:
          Log.e(LOG_TAG, "No data found in channel.");
          callbackDelegate.handleFailure(Constants.JPAKE_ERROR_NODATA);
          break;
        case 412: // "Precondition failed"
          Log.d(LOG_TAG, "Message already replaced on server by other party.");
          callbackDelegate.handleSuccess(response);
          break;
        default:
          Log.e(LOG_TAG, "Could not retrieve data. Server responded with HTTP " + statusCode);
          callbackDelegate.handleFailure(Constants.JPAKE_ERROR_SERVER);
          break;
        }
        // Clean up.
        Log.d(LOG_TAG, "Cleaning up HTTP resources.");
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

      @Override
      public int connectionTimeout() {
        return JPakeClient.REQUEST_TIMEOUT;
      }
    };
    return httpResource;
  }

  /**
   * TimerTask for use with delayed GET requests.
   *
   */
  public class GetStepTimerTask extends TimerTask {
    private Resource request;

    public GetStepTimerTask(Resource request) {
      this.request = request;
    }

    @Override
    public void run() {
      request.get();
    }
  }

  /*
   * Helper method to schedule a GET request with some delay.
   * Basically, run another GetRequestStage.
   */
  private void scheduleGetRequest(int delay, final JPakeClient jClient) {
    timerScheduler.schedule(new TimerTask() {

      @Override
      public void run() {
        new GetRequestStage().execute(jClient);
      }
    }, delay);
  }

}
