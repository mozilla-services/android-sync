package org.mozilla.gecko.tokenserver.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.log.writers.StringLogWriter;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.tokenserver.TokenServerClient;
import org.mozilla.gecko.tokenserver.TokenServerException;
import org.mozilla.gecko.tokenserver.TokenServerException.TokenServerConditionsRequiredException;
import org.mozilla.gecko.tokenserver.TokenServerException.TokenServerInvalidCredentialsException;
import org.mozilla.gecko.tokenserver.TokenServerException.TokenServerMalformedRequestException;
import org.mozilla.gecko.tokenserver.TokenServerException.TokenServerMalformedResponseException;
import org.mozilla.gecko.tokenserver.TokenServerException.TokenServerUnknownServiceException;
import org.mozilla.gecko.tokenserver.TokenServerToken;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.ProtocolVersion;
import ch.boye.httpclientandroidlib.entity.StringEntity;
import ch.boye.httpclientandroidlib.message.BasicHttpResponse;
import ch.boye.httpclientandroidlib.message.BasicStatusLine;

public class TestTokenServerClient {
  public static final String JSON = "application/json";
  public static final String TEXT = "text/plain";

  public static final String TEST_TOKEN_RESPONSE = "{\"api_endpoint\": \"https://stage-aitc1.services.mozilla.com/1.0/1659259\"," +
      "\"duration\": 300," +
      "\"id\": \"eySHORTENED\"," +
      "\"key\": \"-plSHORTENED\"," +
      "\"uid\": 1659259}";

  public static final String TEST_CONDITIONS_RESPONSE = "{\"errors\":[{" +
      "\"location\":\"header\"," +
      "\"description\":\"Need to accept conditions\"," +
      "\"condition_urls\":{\"tos\":\"http://url-to-tos.com\"}," +
      "\"name\":\"X-Conditions-Accepted\"}]," +
      "\"status\":\"error\"}";

  public static final String TEST_ERROR_RESPONSE = "{\"status\": \"error\"," +
      "\"errors\": [{\"location\": \"body\", \"name\": \"\", \"description\": \"Unauthorized EXTENDED\"}]}";

  protected TokenServerClient client;

  @Before
  public void setUp() throws Exception {
    this.client = new TokenServerClient(null); // URI isn't used!
  }

  protected TokenServerToken doProcessResponse(int statusCode, String contentType, Object token)
      throws UnsupportedEncodingException, TokenServerException {
    final HttpResponse response = new BasicHttpResponse(
        new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), statusCode, "OK"));

    StringEntity stringEntity = BaseResource.stringEntity(token.toString());
    stringEntity.setContentType(contentType);
    response.setEntity(stringEntity);

    return client.processResponse(response);
  }

  @SuppressWarnings("rawtypes")
  protected TokenServerException expectProcessResponseFailure(int statusCode, String contentType, Object token, Class klass)
      throws TokenServerException, UnsupportedEncodingException {
    try {
      doProcessResponse(statusCode, contentType, token.toString());
      fail("Expected exception of type " + klass + ".");

      return null;
    } catch (TokenServerException e) {
      assertEquals(klass, e.getClass());

      return e;
    }
  }

  @SuppressWarnings("rawtypes")
  protected TokenServerException expectProcessResponseFailure(Object token, Class klass)
      throws UnsupportedEncodingException, TokenServerException {
    return expectProcessResponseFailure(200, "application/json", token, klass);
  }

  @Test
  public void testProcessResponseSuccess() throws Exception {
    TokenServerToken token = doProcessResponse(200, "application/json", TEST_TOKEN_RESPONSE);

    assertEquals("eySHORTENED", token.id);
    assertEquals("-plSHORTENED", token.key);
    assertEquals("1659259", token.uid);
    assertEquals("https://stage-aitc1.services.mozilla.com/1.0/1659259", token.endpoint);
  }

  @Test
  public void testProcessResponseFailure() throws Exception {
    // Wrong Content-Type.
    expectProcessResponseFailure(200, TEXT, new ExtendedJSONObject(), TokenServerMalformedResponseException.class);

    // Not valid JSON.
    expectProcessResponseFailure("#!", TokenServerMalformedResponseException.class);

    // Status code 400.
    expectProcessResponseFailure(400, JSON, new ExtendedJSONObject(), TokenServerMalformedRequestException.class);

    // Status code 401.
    expectProcessResponseFailure(401, JSON, new ExtendedJSONObject(), TokenServerInvalidCredentialsException.class);

    // Status code 404.
    expectProcessResponseFailure(404, JSON, new ExtendedJSONObject(), TokenServerUnknownServiceException.class);

    // Status code 406, which is not specially handled, but with errors. We take
    // care that errors are actually printed because we're going to want this to
    // work when things go wrong.
    StringLogWriter logWriter = new StringLogWriter();

    Logger.startLoggingTo(logWriter);
    try {
      expectProcessResponseFailure(406, JSON, TEST_ERROR_RESPONSE, TokenServerException.class);

      assertTrue(logWriter.toString().contains("Unauthorized EXTENDED"));
    } finally {
      Logger.stopLoggingTo(logWriter);
    }

    // Status code 503.
    expectProcessResponseFailure(503, JSON, new ExtendedJSONObject(), TokenServerException.class);
  }

  @Test
  public void testProcessResponseConditionsRequired() throws Exception {

    // Status code 403: conditions need to be accepted, but malformed (no urls).
    expectProcessResponseFailure(403, JSON, new ExtendedJSONObject(), TokenServerMalformedResponseException.class);

    // Status code 403, with urls.
    TokenServerConditionsRequiredException e = (TokenServerConditionsRequiredException)
        expectProcessResponseFailure(403, JSON, TEST_CONDITIONS_RESPONSE, TokenServerConditionsRequiredException.class);

    ExtendedJSONObject expectedUrls = new ExtendedJSONObject();
    expectedUrls.put("tos", "http://url-to-tos.com");
    assertEquals(expectedUrls.toString(), e.conditionUrls.toString());
  }

  @Test
  public void testProcessResponseMalformedToken() throws Exception {
    ExtendedJSONObject token;

    // Missing key.
    token = new ExtendedJSONObject(TEST_TOKEN_RESPONSE);
    token.remove("api_endpoint");
    expectProcessResponseFailure(token, TokenServerMalformedResponseException.class);

    // Key has wrong type; expected String.
    token = new ExtendedJSONObject(TEST_TOKEN_RESPONSE);
    token.put("api_endpoint", new ExtendedJSONObject());
    expectProcessResponseFailure(token, TokenServerMalformedResponseException.class);

    // Key has wrong type; expected number.
    token = new ExtendedJSONObject(TEST_TOKEN_RESPONSE);
    token.put("uid", "NON NUMERIC");
    expectProcessResponseFailure(token, TokenServerMalformedResponseException.class);
  }
}
