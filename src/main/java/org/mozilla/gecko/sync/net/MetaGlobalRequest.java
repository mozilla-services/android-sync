/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.net;

import java.net.URISyntaxException;

import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.MetaGlobal;
import org.mozilla.gecko.sync.delegates.MetaGlobalDelegate;

public class MetaGlobalRequest implements SyncStorageRequestDelegate {
  @SuppressWarnings("unused")
  private static final String LOG_TAG = "MetaGlobalRequest";

  protected String metaURL;
  protected String credentials;

  // Temporary location to store our callback.
  private MetaGlobalDelegate callback;

  // A little hack so we can use the same delegate implementation for upload and download.
  private MetaGlobal toUpload;

  public MetaGlobalRequest(String metaURL, String credentials) {
    this.metaURL     = metaURL;
    this.credentials = credentials;
  }

  public void fetch(MetaGlobalDelegate callback) {
    this.callback = callback;
    try {
      this.toUpload = null;
      SyncStorageRecordRequest r = new SyncStorageRecordRequest(this.metaURL);
      r.delegate = this;
      r.deferGet();
    } catch (URISyntaxException e) {
      callback.handleError(e);
    }
  }

  public void upload(MetaGlobal mg, MetaGlobalDelegate callback) {
    try {
      this.toUpload = mg;
      SyncStorageRecordRequest r = new SyncStorageRecordRequest(this.metaURL);

      // TODO: PUT! Body!
      r.delegate = this;
      r.deferPut(null);
    } catch (URISyntaxException e) {
      callback.handleError(e);
    }
  }

  // SyncStorageRequestDelegate methods for fetching.
  public String credentials() {
    return this.credentials;
  }

  public String ifUnmodifiedSince() {
    return null;
  }

  public void handleRequestSuccess(SyncStorageResponse response) {
    if (this.toUpload != null) {
      this.handleUploadSuccess(response);
    } else {
      this.handleDownloadSuccess(response);
    }
  }

  private void handleUploadSuccess(SyncStorageResponse response) {
    this.callback.handleSuccess(this.toUpload, response);
    this.callback = null;
  }

  private void handleDownloadSuccess(SyncStorageResponse response) {
    if (response.wasSuccessful()) {
      try {
        CryptoRecord record = CryptoRecord.fromJSONRecord(response.jsonObjectBody());
        MetaGlobal mg = new MetaGlobal(record);
        this.callback.handleSuccess(mg, response);
        this.callback = null;
      } catch (Exception e) {
        this.callback.handleMalformed(response);
        this.callback = null;
      }
      return;
    }
    this.callback.handleFailure(response);
    this.callback = null;
  }

  public void handleRequestFailure(SyncStorageResponse response) {
    if (this.toUpload == null && response.getStatusCode() == 404) {
      this.callback.handleMissing(response);
      this.callback = null;
      return;
    }
    this.callback.handleFailure(response);
    this.callback = null;
  }

  public void handleRequestError(Exception e) {
    this.callback.handleError(e);
    this.callback = null;
  }
}
