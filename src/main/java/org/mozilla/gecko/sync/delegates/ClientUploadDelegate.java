/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.delegates;

import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.HTTPFailureException;
import org.mozilla.gecko.sync.net.SyncStorageRequestDelegate;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import android.util.Log;

public class ClientUploadDelegate implements SyncStorageRequestDelegate {
  protected static final String LOG_TAG = "ClientUploadDelegate";
  private GlobalSession session;

  public ClientUploadDelegate(GlobalSession session) {
    this.session = session;
  }

  @Override
  public String credentials() {
    return session.credentials();
  }

  @Override
  public String ifUnmodifiedSince() {
    // TODO last client upload time?
    return null;
  }

  @Override
  public void handleRequestSuccess(SyncStorageResponse response) {
    // Response body must be consumed in order to reuse the connection.
    try {
      Log.i(LOG_TAG, "Client upload was successful. Response body: " + response.body());
    } catch (Exception e) {
      session.abort(e, "Unable to print response body");
    }

    session.advance();
  }

  @Override
  public void handleRequestFailure(SyncStorageResponse response) {
    Log.i(LOG_TAG, "Client upload failed. Aborting sync.");
    session.abort(new HTTPFailureException(response), "Client upload failed.");
  }

  @Override
  public void handleRequestError(Exception ex) {
    session.abort(ex, "Client upload failed.");
  }
}