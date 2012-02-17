/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync;

import java.io.IOException;
import java.net.URISyntaxException;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.sync.delegates.MetaGlobalDelegate;
import org.mozilla.gecko.sync.net.SyncStorageRecordRequest;
import org.mozilla.gecko.sync.net.SyncStorageRequestDelegate;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

import android.util.Log;

public class MetaGlobal implements SyncStorageRequestDelegate {
  private static final String LOG_TAG = "MetaGlobal";
  protected String metaURL;
  protected String credentials;

  public boolean isModified;
  protected boolean isNew;

  // Fetched object.
  private CryptoRecord record;

  // Fields.
  protected ExtendedJSONObject  engines;
  protected Long                storageVersion;
  protected String              syncID;

  // Temporary location to store our callback.
  private MetaGlobalDelegate callback;

  // A little hack so we can use the same delegate implementation for upload and download.
  private boolean isUploading;

  public MetaGlobal(String metaURL, String credentials) {
    this.metaURL     = metaURL;
    this.credentials = credentials;
  }

  public void fetch(MetaGlobalDelegate callback) {
    this.callback = callback;
    this.doFetch();
  }

  private void doFetch() {
    try {
      this.isUploading = false;
      SyncStorageRecordRequest r = new SyncStorageRecordRequest(this.metaURL);
      r.delegate = this;
      r.deferGet();
    } catch (URISyntaxException e) {
      callback.handleError(e);
    }
  }

  public void upload(MetaGlobalDelegate callback) {
    try {
      this.isUploading = true;
      SyncStorageRecordRequest r = new SyncStorageRecordRequest(this.metaURL);

      // TODO: PUT! Body!
      r.delegate = this;
      r.deferPut(null);
    } catch (URISyntaxException e) {
      callback.handleError(e);
    }
  }

  private CryptoRecord ensureRecord() {
    if (this.record == null) {
      this.record = new CryptoRecord(new ExtendedJSONObject());
    }
    return this.record;
  }

  protected void setRecord(ExtendedJSONObject obj) throws IOException, ParseException, NonObjectJSONException {
    this.record = CryptoRecord.fromJSONRecord(obj);
  }

  private void unpack(SyncStorageResponse response) throws IllegalStateException, IOException, ParseException, NonObjectJSONException {
    this.setRecord(response.jsonObjectBody());
    Log.i(LOG_TAG, "meta/global is " + record.payload.toJSONString());
    this.isModified = false;
    this.storageVersion = (Long) record.payload.get("storageVersion");
    this.engines  = record.payload.getObject("engines");
    this.syncID = (String) record.payload.get("syncID");
  }

  public Long getStorageVersion() {
    return this.storageVersion;
  }
  public void setStorageVersion(Long version) {
    this.storageVersion = version;
    this.ensureRecord().payload.put("storageVersion", version);
    this.isModified = true;
  }

  public ExtendedJSONObject getEngines() {
    return engines;
  }
  public void setEngines(ExtendedJSONObject engines) {
    this.engines = engines;
    this.ensureRecord().payload.put("engines", engines);
    this.isModified = true;
  }

  public String getSyncID() {
    return syncID;
  }
  public void setSyncID(String syncID) {
    this.syncID = syncID;
    this.ensureRecord().payload.put("syncID", syncID);
    this.isModified = true;
  }

  // SyncStorageRequestDelegate methods for fetching.
  public String credentials() {
    return this.credentials;
  }

  public String ifUnmodifiedSince() {
    return null;
  }

  public void handleRequestSuccess(SyncStorageResponse response) {
    if (this.isUploading) {
      this.handleUploadSuccess(response);
    } else {
      this.handleDownloadSuccess(response);
    }
  }

  private void handleUploadSuccess(SyncStorageResponse response) {
    this.isModified = false;
    this.callback.handleSuccess(this, response);
    this.callback = null;
  }

  private void handleDownloadSuccess(SyncStorageResponse response) {
    if (response.wasSuccessful()) {
      try {
        this.unpack(response);
        this.callback.handleSuccess(this, response);
        this.callback = null;
      } catch (Exception e) {
        this.callback.handleError(e);
        this.callback = null;
      }
      return;
    }
    this.callback.handleFailure(response);
    this.callback = null;
  }

  public void handleRequestFailure(SyncStorageResponse response) {
    if (response.getStatusCode() == 404) {
      this.callback.handleMissing(this, response);
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
