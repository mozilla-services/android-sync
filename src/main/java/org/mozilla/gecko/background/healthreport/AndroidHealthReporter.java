/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.healthreport;

import org.mozilla.gecko.sync.ExtendedJSONObject;


/**
 * Handles FHR data collection from Android providers and organizing the data into JSON objects.
 *
 * @author liuche
 *
 */
public class AndroidHealthReporter {
  // ProviderManager field
  private ExtendedJSONObject jsonPayload = null;

  public AndroidHealthReporter() {
    // TODO: instantiate ProviderManager
  }

  public void collectMeasurements() {
    // TODO: call into ProviderManager to collect data.
  }

  /**
   * Collects up-to-date snapshot of FHR data and obtains a JSON payload.
   */
  public void collectAndObtainJSONPayload() {
    collectMeasurements();
    getJSONPayload();
  }

  /**
   * Obtains a JSON payload from the most recently collected FHR data snapshot.
   */
  public void getJSONPayload() {
    // TODO: generate payload from ContentProviders (or whatever intermediary will be storing FHR data).
  }

  /**
   * Initialize providers manager for handling data providers.
   */
  private void initializeProviderManager() {
    // TODO: hook in ProviderManager implementation.
  }
}
