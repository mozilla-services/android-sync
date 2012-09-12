/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.net;

import java.util.HashMap;

import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.Utils;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.impl.cookie.DateParseException;
import ch.boye.httpclientandroidlib.impl.cookie.DateUtils;

/**
 * An HTTP response from a Sync 1.1 server.
 *
 * @see <a href="http://docs.services.mozilla.com/storage/apis-1.1.html">the Sync 1.1 API documentation</a>.
 */
public class SyncStorageResponse extends SyncResponse {
  public static final String LOG_TAG = "SyncStorageResponse";

  protected static final String HEADER_TIMESTAMP = "X-Weave-Timestamp";
  protected static final String HEADER_RETRY_AFTER = "Retry-After";

  // Responses that are actionable get constant status codes.
  public static final String RESPONSE_CLIENT_UPGRADE_REQUIRED = "16";

  public static HashMap<String, String> SERVER_ERROR_MESSAGES;
  static {
    HashMap<String, String> errors = new HashMap<String, String>();

    // Sync protocol errors.
    errors.put("1", "Illegal method/protocol");
    errors.put("2", "Incorrect/missing CAPTCHA");
    errors.put("3", "Invalid/missing username");
    errors.put("4", "Attempt to overwrite data that can't be overwritten (such as creating a user ID that already exists)");
    errors.put("5", "User ID does not match account in path");
    errors.put("6", "JSON parse failure");
    errors.put("7", "Missing password field");
    errors.put("8", "Invalid Weave Basic Object");
    errors.put("9", "Requested password not strong enough");
    errors.put("10", "Invalid/missing password reset code");
    errors.put("11", "Unsupported function");
    errors.put("12", "No email address on file");
    errors.put("13", "Invalid collection");
    errors.put("14", "User over quota");
    errors.put("15", "The email does not match the username");
    errors.put(RESPONSE_CLIENT_UPGRADE_REQUIRED, "Client upgrade required");
    errors.put("255", "An unexpected server error occurred: pool is empty.");

    // Infrastructure-generated errors.
    errors.put("\"server issue: getVS failed\"",                         "server issue: getVS failed");
    errors.put("\"server issue: prefix not set\"",                       "server issue: prefix not set");
    errors.put("\"server issue: host header not received from client\"", "server issue: host header not received from client");
    errors.put("\"server issue: database lookup failed\"",               "server issue: database lookup failed");
    errors.put("\"server issue: database is not healthy\"",              "server issue: database is not healthy");
    errors.put("\"server issue: database not in pool\"",                 "server issue: database not in pool");
    errors.put("\"server issue: database marked as down\"",              "server issue: database marked as down");
    SERVER_ERROR_MESSAGES = errors;
  }
  public static String getServerErrorMessage(String body) {
    Logger.debug(LOG_TAG, "Looking up message for body \"" + body + "\"");
    if (SERVER_ERROR_MESSAGES.containsKey(body)) {
      return SERVER_ERROR_MESSAGES.get(body);
    }
    return body;
  }

  public SyncStorageResponse(final HttpResponse response) {
    super(response);
  }

  @Override
  public String getErrorMessage() {
    try {
      return SyncStorageResponse.getServerErrorMessage(this.body().trim());
    } catch (Exception e) {
      return null;
    }
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

  /**
   * Return the timestamp header from <code>response</code>, or the current time
   * if it is missing.
   * <p>
   * The timestamp returned from a Sync server is a decimal number of seconds,
   * e.g., 1323393518.04, which we convert to milliseconds since the epoch.
   * <p>
   * <b>Warning:</b> this can cause the timestamp of <code>response</code> to
   * cross domains (from server clock to local clock), which could cause records
   * to be skipped on account of clock drift. This should never happen, because
   * <i>every</i> server response should have a well-formed timestamp header.
   *
   * @return milliseconds since the epoch, as a long, or the current time if the
   *         header was missing or invalid.
   */
  public long getNormalizedTimestamp() {
    long normalizedTimestamp = -1;
    try {
      if (this.hasHeader(HEADER_TIMESTAMP)) {
        normalizedTimestamp = Utils.decimalSecondsToMilliseconds(this.response.getFirstHeader(HEADER_TIMESTAMP).getValue());
      }
    } catch (NumberFormatException e) {
      Logger.warn(LOG_TAG, "Malformed timestamp header received.", e);
    }

    if (-1 == normalizedTimestamp) {
      Logger.warn(LOG_TAG, "Computing stand-in timestamp from local clock. Clock drift could cause records to be skipped.");
      normalizedTimestamp = System.currentTimeMillis();
    }

    return normalizedTimestamp;
  }

  /**
   * @return A number of seconds, or -1 if the 'Retry-After' header was not present.
   */
  @Override
  public int retryAfterInSeconds() throws NumberFormatException {
    if (!this.hasHeader(HEADER_RETRY_AFTER)) {
      return -1;
    }

    Header header = this.response.getFirstHeader(HEADER_RETRY_AFTER);
    String retryAfter = header.getValue();
    if (missingHeader(retryAfter)) {
      Logger.warn(LOG_TAG, "Retry-After header present but empty.");
      return -1;
    }

    try {
      return Integer.parseInt(retryAfter, 10);
    } catch (NumberFormatException e) {
      // Fall through to try date format.
    }

    try {
      final long then = DateUtils.parseDate(retryAfter).getTime();
      final long now  = System.currentTimeMillis();
      return (int)((then - now) / 1000);     // Convert milliseconds to seconds.
    } catch (DateParseException e) {
      Logger.warn(LOG_TAG, "Retry-After header neither integer nor date: " + retryAfter);
      return -1;
    }
  }

  /**
   * @return A number of seconds, or -1 if the 'X-Weave-Backoff' header was not
   *         present.
   */
  @Override
  public int backoffInSeconds() throws NumberFormatException {
    return this.getIntegerHeader("x-weave-backoff");
  }

  /**
   * @return A number of milliseconds, or -1 if neither the 'Retry-After' or
   *         'X-Weave-Backoff' header was present.
   */
  @Override
  public long totalBackoffInMilliseconds() {
    int retryAfterInSeconds = -1;
    try {
      retryAfterInSeconds = retryAfterInSeconds();
    } catch (NumberFormatException e) {
    }

    int weaveBackoffInSeconds = -1;
    try {
      weaveBackoffInSeconds = backoffInSeconds();
    } catch (NumberFormatException e) {
    }

    long totalBackoff = (long) Math.max(retryAfterInSeconds, weaveBackoffInSeconds);
    if (totalBackoff < 0) {
      return -1;
    } else {
      return 1000 * totalBackoff;
    }
  }

  // TODO: Content-Type and Content-Length validation.

}
