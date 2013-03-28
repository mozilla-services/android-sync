/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.healthreport;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.mozilla.gecko.sync.ExtendedJSONObject;

import android.annotation.SuppressLint;


/**
 * Handles FHR data collection from Android providers and organizing the data into JSON objects.
 *
 * @author liuche
 *
 */
public class AndroidHealthReporter {
  // ProviderManager field
  private final int APP_INFO_VERSION = 1;
  private ExtendedJSONObject appInfoFields;

  public AndroidHealthReporter() {
    // TODO: instantiate ProviderManager
  }

  public void collectMeasurements() {
    // TODO: call into ProviderManager to collect data.
  }

  /**
   * Collects up-to-date snapshot of FHR data and obtains a JSON payload.
   */
  public ExtendedJSONObject collectAndObtainJSONPayload() {
    collectMeasurements();
    return getJSONPayload();
  }

  /**
   * Obtains a JSON payload from the most recently collected FHR data snapshot.
   */
  public ExtendedJSONObject getJSONPayload() {
    ExtendedJSONObject payload = new ExtendedJSONObject();
    setPayloadMetadata(payload);
    // TODO: generate payload from ContentProviders (or whatever intermediary will be storing FHR data).
    
    return payload;
  }

  @SuppressLint("SimpleDateFormat")
  private void setPayloadMetadata(ExtendedJSONObject payload) {
    Calendar calendar = Calendar.getInstance();
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String formattedDate = dateFormat.format(calendar.getTime());

    payload.put("version", 2);
    payload.put("thisPingDate", formattedDate);
    payload.put("geckoAppInfo", obtainAppInfo());

    ExtendedJSONObject data = new ExtendedJSONObject();
    data.put("last", new ExtendedJSONObject());
    data.put("days", new ExtendedJSONObject());
    payload.put("data", data);
  }

  private ExtendedJSONObject obtainAppInfo() {
    ExtendedJSONObject appInfoJSON = new ExtendedJSONObject();
    appInfoJSON.put("_v", this.APP_INFO_VERSION);
    ensureAppInfoFields();
    // TODO: process and fetch appInfoFields from corresponding Java version of Services.appinfo.
    return appInfoJSON;
  }

  private void ensureAppInfoFields() {
    if (this.appInfoFields == null) {
      this.appInfoFields = new ExtendedJSONObject();
      appInfoFields.put("vendor", "vendor");
      appInfoFields.put("name", "name");
      appInfoFields.put("id", "ID");
      appInfoFields.put("version", "version");
      appInfoFields.put("appBuildID", "appBuildID");
      appInfoFields.put("platformVersion", "platformVersion");
      appInfoFields.put("platformBuildID", "platformBuildID");
      appInfoFields.put("os", "OS");
      appInfoFields.put("xpcomabi", "XPCOMABI");
    }
  }
  /**
   * Initialize providers manager for handling data providers.
   */
  private void initializeProviderManager() {
    // TODO: hook in ProviderManager implementation.
  }
}
