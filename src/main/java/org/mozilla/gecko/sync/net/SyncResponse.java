package org.mozilla.gecko.sync.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.Scanner;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;

public class SyncResponse {

  protected HttpResponse response;

  public SyncResponse() {
    super();
  }

  public SyncResponse(HttpResponse res) {
    response = res;
  }

  public HttpResponse httpResponse() {
    return this.response;
  }

  public int getStatusCode() {
    return this.response.getStatusLine().getStatusCode();
  }

  public boolean wasSuccessful() {
    return this.getStatusCode() == 200;
  }

  public String body() throws IllegalStateException, IOException {
    InputStreamReader is = new InputStreamReader(this.response.getEntity().getContent());
    // Oh, Java, you are so evil.
    return new Scanner(is).useDelimiter("\\A").next();
  }

  /**
   * Return the body as an Object.
   *
   * @return null if there is no body, or an Object if it successfully parses.
   *         The return value will be an ExtendedJSONObject if it's a JSON object.
   * @throws IllegalStateException
   * @throws IOException
   * @throws ParseException
   */
  public Object jsonBody() throws IllegalStateException, IOException,
                          ParseException {
    HttpEntity entity = this.response.getEntity();
    if (entity == null) {
      return null;
    }
    InputStream content = entity.getContent();
    return ExtendedJSONObject.parse(content);
  }

  public ExtendedJSONObject jsonObjectBody() throws IllegalStateException,
                                            IOException, ParseException,
                                            NonObjectJSONException {
    Object body = this.jsonBody();
    if (body instanceof ExtendedJSONObject) {
      return (ExtendedJSONObject) body;
    }
    throw new NonObjectJSONException(body);
  }

  private boolean hasHeader(String h) {
    return this.response.containsHeader(h);
  }

  private int getIntegerHeader(String h) {
    if (this.hasHeader(h)) {
      Header header = this.response.getFirstHeader(h);
      return Integer.parseInt(header.getValue(), 10);
    }
    return -1;
  }

  /**
   * @return A number of seconds, or -1 if the header was not present.
   */
  public int retryAfter() throws NumberFormatException {
    return this.getIntegerHeader("retry-after");
  }

  public int weaveBackoff() throws NumberFormatException {
    return this.getIntegerHeader("x-weave-backoff");
  }

  // This lives until Bug 708956 lands, and we don't have to do it any more.
  public static long decimalSecondsToMilliseconds(String decimal) {
    try {
      return new BigDecimal(decimal).movePointRight(3).longValue();
    } catch (Exception e) {
      return -1;
    }
  }

  /**
   * The timestamp returned from a Sync server is a decimal number of seconds,
   * e.g., 1323393518.04.
   *
   * We want milliseconds since epoch.
   *
   * @return milliseconds since the epoch, as a long, or -1 if the header
   *         was missing or invalid.
   */
  public long normalizedWeaveTimestamp() {
    String h = "x-weave-timestamp";
    if (!this.hasHeader(h)) {
      return -1;
    }

    return decimalSecondsToMilliseconds(this.response.getFirstHeader(h).getValue());
  }

  public int weaveRecords() throws NumberFormatException {
    return this.getIntegerHeader("x-weave-records");
  }

  public int weaveQuotaRemaining() throws NumberFormatException {
    return this.getIntegerHeader("x-weave-quota-remaining");
  }

  public String weaveAlert() {
    if (this.hasHeader("x-weave-alert")) {
      return this.response.getFirstHeader("x-weave-alert").getValue();
    }
    return null;
  }

}