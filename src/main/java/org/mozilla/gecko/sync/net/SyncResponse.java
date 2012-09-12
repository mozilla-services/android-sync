/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.NonObjectJSONException;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;

/**
 * An abstract HTTP response from a Sync server.
 * <p>
 * All Sync responses provide:
 * <ul>
 * <li>a status code;</li>
 * <li>a server specific error message;</li>
 * <li>access to the response body;</li>
 * <li>access to server specific retry after and backoff headers.</li>
 * </ul>
 */
public abstract class SyncResponse {
  private static final String LOG_TAG = "SyncResponse";

  protected final HttpResponse response;
  protected String body = null;

  public SyncResponse(final HttpResponse res) {
    response = res;
  }

  public HttpResponse httpResponse() {
    return this.response;
  }

  public int getStatusCode() {
    return this.response.getStatusLine().getStatusCode();
  }

  public String body() throws IllegalStateException, IOException {
    if (body != null) {
      return body;
    }
    InputStreamReader is = new InputStreamReader(this.response.getEntity().getContent());
    // Oh, Java, you are so evil.
    body = new Scanner(is).useDelimiter("\\A").next();
    return body;
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
    if (body != null) {
      // Do it from the cached String.
      return ExtendedJSONObject.parse(body);
    }

    HttpEntity entity = this.response.getEntity();
    if (entity == null) {
      return null;
    }
    InputStream content = entity.getContent();
    try {
      return ExtendedJSONObject.parse(content);
    } finally {
      content.close();
    }
  }

  /**
   * Return the body as a <b>non-null</b> <code>ExtendedJSONObject</code>.
   *
   * @return A non-null <code>ExtendedJSONObject</code>.
   *
   * @throws IllegalStateException
   * @throws IOException
   * @throws ParseException
   * @throws NonObjectJSONException
   */
  public ExtendedJSONObject jsonObjectBody() throws IllegalStateException,
                                            IOException, ParseException,
                                            NonObjectJSONException {
    Object body = this.jsonBody();
    if (body instanceof ExtendedJSONObject) {
      return (ExtendedJSONObject) body;
    }
    throw new NonObjectJSONException(body);
  }

  protected boolean hasHeader(String h) {
    return this.response.containsHeader(h);
  }

  protected static boolean missingHeader(String value) {
    return value == null ||
           value.trim().length() == 0;
  }

  protected int getIntegerHeader(String h) throws NumberFormatException {
    if (this.hasHeader(h)) {
      Header header = this.response.getFirstHeader(h);
      String value  = header.getValue();
      if (missingHeader(value)) {
        Logger.warn(LOG_TAG, h + " header present but empty.");
        return -1;
      }
      return Integer.parseInt(value, 10);
    }
    return -1;
  }

  /**
   * Return any server specific error message.
   *
   * @return error message, or null if none was present or well formed.
   */
  public abstract String getErrorMessage();

  /**
   * Return the timestamp from this response, or the current time if it is
   * missing.
   *
   * @return milliseconds since the epoch, as a long, or the current time if the
   *         timestamp was missing or invalid.
   */
  public abstract long getNormalizedTimestamp();

  /**
   * Query the server retry after header.
   *
   * @return number of seconds, or -1 if the retry after header was malformed or missing.
   */
  public abstract int retryAfterInSeconds();

  /**
   * Query the server backoff header.
   *
   * @return number of seconds, or -1 if the backoff header was malformed or missing.
   */
  public abstract int backoffInSeconds();

  /**
   * Get the maximum the server retry and backoff headers.
   *
   * @return number of milliseconds, or -1 if neither the retry after or backoff header was well formed or present.
   */
  public abstract long totalBackoffInMilliseconds();
}
