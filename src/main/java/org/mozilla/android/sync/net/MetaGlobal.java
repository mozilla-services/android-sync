package org.mozilla.android.sync.net;

import java.io.IOException;
import java.net.URISyntaxException;

import org.json.simple.parser.ParseException;
import org.mozilla.android.sync.ExtendedJSONObject;
import org.mozilla.android.sync.NonObjectJSONException;

public class MetaGlobal implements SyncStorageRequestDelegate {
  protected String metaURL;
  protected String credentials;

  public boolean isModified;
  protected boolean isNew;

  // Fetched objects.
  protected SyncStorageResponse response;
  private ExtendedJSONObject  record;

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
    if (this.response == null) {
      this.callback = callback;
      this.doFetch();
      return;
    }
    callback.handleSuccess(this);
  }

  private void doFetch() {
    try {
      this.isUploading = false;
      SyncStorageRecordRequest r = new SyncStorageRecordRequest(this.metaURL);
      r.delegate = this;
      r.get();
    } catch (URISyntaxException e) {
      callback.handleError(e);
    }
  }

  public SyncStorageResponse getResponse() {
    return this.response;
  }

  public void upload(MetaGlobalDelegate callback) {
    try {
      this.isUploading = true;
      SyncStorageRecordRequest r = new SyncStorageRecordRequest(this.metaURL);
      r.delegate = this;
      r.get();
    } catch (URISyntaxException e) {
      callback.handleError(e);
    }
  }

  protected ExtendedJSONObject ensureRecord() {
    if (record == null) {
      record = new ExtendedJSONObject();
    }
    return record;
  }

  protected void setRecord(ExtendedJSONObject record) {
    this.record = record;
  }

  private void unpack(SyncStorageResponse response) throws IllegalStateException, IOException, ParseException, NonObjectJSONException {
    this.response = response;
    //System.out.println("Response: " + response.body());
    this.setRecord(response.jsonObjectBody());
    this.isModified = false;
    this.storageVersion = (Long) ensureRecord().get("storageVersion");
    this.engines  = ensureRecord().getObject("engines");
    this.syncID = (String) ensureRecord().get("syncID");
  }

  public Long getStorageVersion() {
    return this.storageVersion;
  }
  public void setStorageVersion(Long version) {
    this.storageVersion = version;
    this.ensureRecord().put("storageVersion", version);
    this.isModified = true;
  }

  public ExtendedJSONObject getEngines() {
    return engines;
  }
  public void setEngines(ExtendedJSONObject engines) {
    this.engines = engines;
    this.ensureRecord().put("engines", engines);
    this.isModified = true;
  }

  public String getSyncID() {
    return syncID;
  }
  public void setSyncID(String syncID) {
    this.syncID = syncID;
    this.ensureRecord().put("syncID", syncID);
    this.isModified = true;
  }

  // SyncStorageRequestDelegate methods for fetching.
  public String credentials() {
    return this.credentials;
  }

  public String ifUnmodifiedSince() {
    return null;
  }

  public void handleSuccess(SyncStorageResponse response) {
    if (this.isUploading) {
      this.handleUploadSuccess(response);
    } else {
      this.handleDownloadSuccess(response);
    }
  }

  private void handleUploadSuccess(SyncStorageResponse response) {
    this.isModified = false;
    this.callback.handleSuccess(this);
    this.callback = null;
  }

  private void handleDownloadSuccess(SyncStorageResponse response) {
    if (response.wasSuccessful()) {
      try {
        this.unpack(response);
        this.callback.handleSuccess(this);
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

  public void handleFailure(SyncStorageResponse response) {
    if (response.getStatusCode() == 404) {
      this.response = response;
      this.callback.handleMissing(this);
      this.callback = null;
      return;
    }
    this.callback.handleFailure(response);
    this.callback = null;
  }

  public void handleError(IOException e) {
    this.callback.handleError(e);
    this.callback = null;
  }
}
