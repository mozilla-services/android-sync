/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Android Sync Client.
 *
 * The Initial Developer of the Original Code is
 * the Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *  Richard Newman <rnewman@mozilla.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.android.sync.net;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import org.json.simple.parser.ParseException;
import org.mozilla.android.sync.ExtendedJSONObject;
import org.mozilla.android.sync.NonObjectJSONException;

public class SyncStorageResponse {
  // Server responses on which we want to switch.
  static final int SERVER_RESPONSE_OVER_QUOTA = 14;

  // Higher-level interpretations of response contents.
  public enum Reason {
    SUCCESS,
    OVER_QUOTA,
    UNAUTHORIZED_OR_REASSIGNED,
    SERVICE_UNAVAILABLE,
    BAD_REQUEST,
    UNKNOWN
  }

  private HttpResponse response;

  public SyncStorageResponse(HttpResponse res) {
    this.response = res;
  }

  public HttpResponse httpResponse() {
    return this.response;
  }

  public boolean wasSuccessful() {
    return this.response.getStatusLine().getStatusCode() == 200;
  }

  /**
   * Return the high-level definition of the status of this request.
   * @return
   */
  public Reason reason() {
    switch (this.response.getStatusLine().getStatusCode()) {
    case 200:
      return Reason.SUCCESS;
    case 400:
      try {
        Object body = this.jsonBody();
        if (body instanceof Number) {
          if (((Number) body).intValue() == SERVER_RESPONSE_OVER_QUOTA) {
            return Reason.OVER_QUOTA;
          }
        }
      } catch (Exception e) {
      }
      return Reason.BAD_REQUEST;
    case 401:
      return Reason.UNAUTHORIZED_OR_REASSIGNED;
    case 503:
      return Reason.SERVICE_UNAVAILABLE;
    }
    return Reason.UNKNOWN;
  }

  // TODO: Content-Type and Content-Length validation.

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
    return ExtendedJSONObject.parse(entity.getContent());
  }

  public ExtendedJSONObject jsonObjectBody() throws IllegalStateException, IOException, ParseException, NonObjectJSONException {
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
      Header header = this.response.getFirstHeader("retry-after");
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

  public int weaveTimestamp() throws NumberFormatException {
    return this.getIntegerHeader("x-weave-timestamp");
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
