/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.sync.net.SyncResponse;

import android.content.SharedPreferences;

/**
 * Handles the processing of an end-of-life message from a Sync server.
 *
 * Provide a delegate for message display and account disablement.
 */
public class EndOfLifeHandler {
  private static final String LOG_TAG = EndOfLifeHandler.class.getName();

  public enum EOLCode {
    SOFT("soft-eol"),
    HARD("hard-eol");

    private final String code;

    private EOLCode(final String code) {
      this.code = code;
    }

    @Override
    public String toString() {
      return code;
    }

    public static EOLCode forCode(final String code) {
      if (SOFT.code.equals(code)) {
        return SOFT;
      }
      if (HARD.code.equals(code)) {
        return HARD;
      }
      throw new IllegalArgumentException("No such EOL code " + code);
    }
  }

  public interface EOLDelegate {
    public void disableAccount();
    public void displayMessageForEOLCode(EOLCode code, String url);
  }

  private static final String XWA_CODE = "code";
  private static final String XWA_URL = "url";
  private static final String XWA_MESSAGE = "message";

  public static final long EOL_ALERT_INTERVAL_MSEC = 7 * 24 * 60 * 60 * 1000;   // One week.

  public static final String PREF_EOL_STATE = "errorhandler.alert.mode";
  public static final String PREF_EARLIEST_NEXT = "errorhandler.alert.earliestNext";

  private final SharedPreferences prefs;
  private final EOLDelegate delegate;

  public EndOfLifeHandler(SharedPreferences prefs, EOLDelegate delegate) {
    this.prefs = prefs;
    this.delegate = delegate;
  }

  /**
   * Process a received HTTP response, checking for deprecation indicators.
   */
  public void handleServerResponse(final SyncResponse response) {
    final int statusCode = response.getStatusCode();
    if (statusCode != 200 &&
        statusCode != 404 &&
        statusCode != 513) {
      return;
    }

    final String alert = response.weaveAlert();
    if (alert == null) {
      return;
    }

    handleServerAlert(alert);
  }

  /**
   * Parses a structured X-Weave-Alert header from the server, emitting
   * end-of-life notifications if necessary.
   *
   * @param xwa
   *          a JSON string, with keys <code>code</code>, <code>url</code>,
   *          <code>message</code>.
   */
  public void handleServerAlert(final String xwa) {
    final JSONObject o;
    try {
      o = new JSONObject(xwa);
    } catch (Exception e) {
      // Nothing we can do.
      return;
    }

    if (!o.has(XWA_CODE)) {
      // Malformed.
      Logger.error(LOG_TAG, "No EOL code in server alert.");
      return;
    }

    final EOLCode code;
    try {
      code = EOLCode.forCode(o.getString(XWA_CODE));
    } catch (JSONException e) {
      Logger.error(LOG_TAG, "Malformed EOL code in server alert.");
      return;
    } catch (IllegalArgumentException ex) {
      // Unexpected code.
      Logger.error(LOG_TAG, "Unexpected EOL code.");
      return;
    }

    String url = o.optString(XWA_URL);
    String message = o.optString(XWA_MESSAGE);

    handleServerAlert(code, url, message);
  }

  /**
   * When we see a server alert, we decide what to do based on:
   *
   * 1. Our saved EOL state. 2. If we've alerted before, and when. 3. What the
   * server code is.
   *
   * If we're in a soft-eol state, we remind infrequently.
   *
   * If we're in a soft-eol state and we get a hard-eol, we remind again, with a
   * different message, and we deactivate your Sync account.
   *
   * @param url
   *          a "Learn more" link. On Android we take a user to Google Play by
   *          preference.
   * @param message
   *          a log message to record.
   */
  private void handleServerAlert(EOLCode code, String url, String message) {
    Logger.error(LOG_TAG, "X-Weave-Alert: " + code + ": " + message);
    if (updateAlertMode(code) ||
        hasAlertIntervalExpired()) {
      extendAlertInterval();
      delegate.displayMessageForEOLCode(code, url);
    }

    if (EOLCode.HARD == code) {
      delegate.disableAccount();
    }
  }

  /**
   * @return true if the minimum interval has passed since we last showed an EOL
   *         message.
   */
  private boolean hasAlertIntervalExpired() {
    return prefs.getLong(PREF_EARLIEST_NEXT, 0L) < now();
  }

  private void extendAlertInterval() {
    prefs.edit()
         .putLong(PREF_EARLIEST_NEXT, now() + EOL_ALERT_INTERVAL_MSEC)
         .commit();
  }

  /**
   * Update our stored EOL alert state.
   *
   * @param code
   *          an EOL code ("hard-eol", "soft-eol")
   * @return true if the alert state changed.
   */
  private boolean updateAlertMode(EOLCode code) {
    String current = prefs.getString(PREF_EOL_STATE, null);
    if (code.toString().equals(current)) {
      return false;
    }

    prefs.edit()
         .putString(PREF_EOL_STATE, code.toString())
         .commit();
    return true;
  }

  private long now() {
    return System.currentTimeMillis();
  }
}
