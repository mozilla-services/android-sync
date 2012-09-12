package org.mozilla.gecko.sync.net.server11;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;

import ch.boye.httpclientandroidlib.HttpEntity;

public abstract class SyncServerRequest {
  public static final String LOG_TAG = "SyncServerRequest";

  protected BaseResourceDelegate resourceDelegate;
  protected BaseResource resource;

  protected volatile boolean aborting = false;

  /**
   * Instruct the request that it should process no more records, and decline to
   * notify any more delegate callbacks.
   */
  public void abort() {
    aborting = true;
    try {
      this.resource.abort();
    } catch (Exception e) {
      // Just in case.
      Logger.warn(LOG_TAG, "Got exception in abort: " + e);
    }
  }

  public void get() {
    this.resource.get();
  }

  public void delete() {
    this.resource.delete();
  }

  public void post(HttpEntity body) {
    this.resource.post(body);
  }

  public void put(HttpEntity body) {
    this.resource.put(body);
  }

  public void post(JSONObject body) {
    this.resource.post(body);
  }

  public void post(JSONArray body) {
    this.resource.post(body);
  }

  public void put(JSONObject body) {
    this.resource.put(body);
  }

  public void post(CryptoRecord record) {
    this.post(record.toJSONObject());
  }

  public void put(CryptoRecord record) {
    this.put(record.toJSONObject());
  }
}