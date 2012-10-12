package org.mozilla.gecko.tokenserver;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.NonArrayJSONException;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;
import org.mozilla.gecko.sync.net.BrowserIDAuthHeaderProvider;
import org.mozilla.gecko.sync.net.SyncResponse;
import org.mozilla.gecko.tokenserver.TokenServerException.TokenServerConditionsRequiredException;
import org.mozilla.gecko.tokenserver.TokenServerException.TokenServerInvalidCredentialsException;
import org.mozilla.gecko.tokenserver.TokenServerException.TokenServerMalformedRequestException;
import org.mozilla.gecko.tokenserver.TokenServerException.TokenServerMalformedResponseException;
import org.mozilla.gecko.tokenserver.TokenServerException.TokenServerUnknownServiceException;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.message.BasicHeader;

public class TokenServerClient {
  public static final String LOG_TAG = "TokenServerClient";

  public static final String JSON_KEY_ID = "id";
  public static final String JSON_KEY_KEY = "key";
  public static final String JSON_KEY_ENDPOINT = "api_endpoint";
  public static final String JSON_KEY_UID = "uid";
  public static final String JSON_KEY_DURATION = "duration";

  public static final String JSON_KEY_ERRORS = "errors";
  public static final String JSON_KEY_CONDITION_URLS = "condition_urls";

  protected final URI uri;

  public TokenServerClient(URI uri) {
    this.uri = uri;
  }

  public TokenServerToken processResponse(HttpResponse response)
      throws TokenServerException {
    SyncResponse res = new SyncResponse(response);
    int statusCode = res.getStatusCode();

    Logger.debug(LOG_TAG, "Got token response with status code " + statusCode + ".");

    // Responses should *always* be JSON, even in the case of 4xx and 5xx
    // errors. If we don't see JSON, the server is likely very unhappy.
    String contentType = response.getEntity().getContentType().getValue();
    if (contentType != "application/json" && !contentType.startsWith("application/json;")) {
      Logger.warn(LOG_TAG, "Got non-JSON response with Content-Type " +
          contentType + ". Misconfigured server?");

      String body;
      try {
        body = res.body();
        Logger.pii(LOG_TAG, "Body: " + body);
      } catch (Exception e) {
        // Do nothing -- this is just logging.
      }

      throw new TokenServerMalformedResponseException(null, "Non-JSON response Content-Type.");
    }

    // Responses should *always* be a valid JSON object.
    ExtendedJSONObject result;
    try {
      result = res.jsonObjectBody();
    } catch (Exception e) {
      Logger.debug(LOG_TAG, "Malformed token response.", e);
      throw new TokenServerMalformedResponseException(null, e);
    }

    // The service shouldn't have any 3xx, so we don't need to handle those.
    if (res.getStatusCode() != 200) {
      // We should have a (Cornice) error report in the JSON. We log that to
      // help with debugging.
      List<ExtendedJSONObject> errorList = new ArrayList<ExtendedJSONObject>();

      if (result.containsKey(JSON_KEY_ERRORS)) {
        try {
          for (Object error : result.getArray(JSON_KEY_ERRORS)) {
            Logger.warn(LOG_TAG, "" + error);

            if (error instanceof JSONObject) {
              errorList.add(new ExtendedJSONObject((JSONObject) error));
            }
          }
        } catch (NonArrayJSONException e) {
          Logger.warn(LOG_TAG, "Got non-JSON array '" + result.getString(JSON_KEY_ERRORS) + "'.", e);
        }
      }

      if (statusCode == 400) {
        throw new TokenServerMalformedRequestException(errorList);
      }

      if (statusCode == 401) {
        throw new TokenServerInvalidCredentialsException(errorList);
      }

      // 403 should represent a "condition acceptance needed" response.
      //
      // The extra validation of "urls" is important. We don't want to signal
      // conditions required unless we are absolutely sure that is what the
      // server is asking for.
      if (statusCode == 403) {
        // Bug 792674 and Bug 783598: make this testing simpler. For now, we
        // check that errors is an array, and take any condition_urls from the
        // first element.

        try {
          if (errorList == null || errorList.isEmpty()) {
            throw new TokenServerMalformedResponseException(errorList, "403 response without proper fields.");
          }

          ExtendedJSONObject error = errorList.get(0);

          ExtendedJSONObject condition_urls = error.getObject(JSON_KEY_CONDITION_URLS);
          if (condition_urls != null) {
            throw new TokenServerConditionsRequiredException(condition_urls);
          }
        } catch (NonObjectJSONException e) {
          Logger.warn(LOG_TAG, "Got non-JSON error object.");
        }

        throw new TokenServerMalformedResponseException(errorList, "403 response without proper fields.");
      }

      if (statusCode == 404) {
        throw new TokenServerUnknownServiceException(errorList);
      }

      // We shouldn't ever get here...
      throw new TokenServerException(errorList);
    }

    // Defensive as possible: verify object has expected keys with non-null string values.
    for (String k : new String[] { JSON_KEY_ID, JSON_KEY_KEY, JSON_KEY_ENDPOINT }) {
      Object value = result.get(k);
      if (value == null) {
        throw new TokenServerMalformedResponseException(null, "Expected key not present in result: " + k);
      }
      if (!(value instanceof String)) {
        throw new TokenServerMalformedResponseException(null, "Value for key not a string in result: " + k);
      }
    }

    // Defensive as possible: verify object has expected key(s) with non-null value.
    for (String k : new String[] { JSON_KEY_UID }) {
      Object value = result.get(k);
      if (value == null) {
        throw new TokenServerMalformedResponseException(null, "Expected key not present in result: " + k);
      }
      if (!(value instanceof Long)) {
        throw new TokenServerMalformedResponseException(null, "Value for key not a string in result: " + k);
      }
    }

    Logger.debug(LOG_TAG, "Successful token response: " + result.getString(JSON_KEY_ID));

    return new TokenServerToken(result.getString(JSON_KEY_ID),
        result.getString(JSON_KEY_KEY),
        result.get(JSON_KEY_UID).toString(),
        result.getString(JSON_KEY_ENDPOINT));
  }

  public void getTokenFromBrowserIDAssertion(final String assertion, final boolean conditionsAccepted,
      final TokenServerClientDelegate delegate) {
    BaseResource r = new BaseResource(uri);

    r.delegate = new BaseResourceDelegate(r) {
      @Override
      public void handleHttpResponse(HttpResponse response) {
        try {
          TokenServerToken token = processResponse(response);
          delegate.handleSuccess(token);
        } catch (TokenServerException e) {
          delegate.handleFailure(e);
        }
      }

      @Override
      public void handleTransportException(GeneralSecurityException e) {
        delegate.handleError(e);
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
      public AuthHeaderProvider getAuthHeaderProvider() {
        return new BrowserIDAuthHeaderProvider(assertion);
      }

      @Override
      public void addHeaders(HttpRequestBase request, DefaultHttpClient client) {
        String host = request.getURI().getHost();
        request.setHeader(new BasicHeader("Host", host));

        if (conditionsAccepted) {
          request.addHeader("X-Conditions-Accepted", "1");
        }
      }
    };

    r.get();
  }
}
