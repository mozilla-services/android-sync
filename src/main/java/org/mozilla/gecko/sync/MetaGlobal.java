/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.mozilla.gecko.sync.MetaGlobalException.MetaGlobalMalformedSyncIDException;
import org.mozilla.gecko.sync.MetaGlobalException.MetaGlobalMalformedVersionException;
import org.mozilla.gecko.sync.MetaGlobalException.MetaGlobalStaleClientSyncIDException;
import org.mozilla.gecko.sync.MetaGlobalException.MetaGlobalStaleClientVersionException;

public class MetaGlobal {
  @SuppressWarnings("unused")
  private static final String LOG_TAG = "MetaGlobal";

  protected Map<String, EngineSettings> engines = new HashMap<String, EngineSettings>();
  public String syncID;
  public int storageVersion;

  public MetaGlobal() {
  }

  public MetaGlobal(CryptoRecord record) throws MetaGlobalException {
    try {
      setFromRecord(record);
    } catch (Exception e) {
      throw new MetaGlobalException();
    }
  }

  public Collection<String> getEngineNames() {
    return new ArrayList<String>(engines.keySet());
  }

  public EngineSettings getEngineSettings(String engineName) {
    return engines.get(engineName);
  }

  protected ExtendedJSONObject asRecordContents() {
    ExtendedJSONObject json = new ExtendedJSONObject();
    json.put("syncID", syncID);
    json.put("storageVersion", storageVersion);
    ExtendedJSONObject jsonEngines = new ExtendedJSONObject();
    for (Entry<String, EngineSettings> pair : engines.entrySet()) {
      jsonEngines.put(pair.getKey(), pair.getValue().toJSONObject());
    }
    json.put("engines", jsonEngines);
    return json;
  }

  public CryptoRecord asCryptoRecord() {
    ExtendedJSONObject payload = this.asRecordContents();
    CryptoRecord record = new CryptoRecord(payload);
    record.collection = "meta";
    record.guid       = "global";
    record.deleted    = false;
    return record;
  }

  public void setFromRecord(CryptoRecord record) throws MetaGlobalException {
    if (record == null || record.payload == null) {
      throw new IllegalArgumentException("record and record.payload cannot be null.");
    }
    // Log.i(LOG_TAG, "meta/global is " + record.payload.toJSONString());

    try {
      this.syncID = record.payload.getString("syncID");
    } catch (Exception e) {
      throw new MetaGlobalException.MetaGlobalMalformedSyncIDException();
    }
    if (this.syncID == null) {
      throw new MetaGlobalException.MetaGlobalMalformedSyncIDException();
    }

    try {
      Integer storageVersion = record.payload.getIntegerSafely("storageVersion");
      if (storageVersion == null) {
        throw new MetaGlobalException.MetaGlobalMalformedVersionException();
      }
      this.storageVersion = storageVersion.intValue();
      if (this.storageVersion == 0) {
        throw new MetaGlobalException.MetaGlobalMalformedVersionException();
      }
    } catch (NumberFormatException e) {
      throw new MetaGlobalException.MetaGlobalMalformedVersionException();
    }

    ExtendedJSONObject jsonEngines;
    try {
      jsonEngines = record.payload.getObject("engines");
    } catch (NonObjectJSONException e) {
      throw new MetaGlobalException.MetaGlobalMissingEnginesException();
    }
    if (jsonEngines == null) {
      throw new MetaGlobalException.MetaGlobalMissingEnginesException();
    }

    for (Entry<String, Object> pair : jsonEngines.entryIterable()) {
      String engineName = pair.getKey();
      ExtendedJSONObject jsonEngine = null;
      try {
        jsonEngine = jsonEngines.getObject(engineName);
      } catch (NonObjectJSONException e) {
        throw new MetaGlobalException.MetaGlobalMalformedEnginesException();
      }
      if (jsonEngine == null) {
        continue; // Possibly should throw here.
      }
      try {
        EngineSettings engineSettings = new EngineSettings(jsonEngine);
        this.engines.put(engineName, engineSettings);
      } catch (Exception e) {
        continue; // Possible should throw here.
      }
    }
  }

  /**
   * Returns if the server settings and local settings match.
   * Throws a specific exception if that's not the case.
   */
  public static void verifyEngineSettings(EngineSettings serverSettings,
                                          EngineSettings clientSettings)
  throws MetaGlobalMalformedVersionException, MetaGlobalMalformedSyncIDException, MetaGlobalStaleClientVersionException, MetaGlobalStaleClientSyncIDException {

    if (serverSettings == null) {
      throw new IllegalArgumentException("serverSettings cannot be null.");
    }
    if (clientSettings == null) {
      throw new IllegalArgumentException("engineSettings cannot be null.");
    }

    if (serverSettings.version < 1) {
      // Invalid version. Wipe the server.
      throw new MetaGlobalException.MetaGlobalMalformedVersionException();
    }

    if (serverSettings.version > clientSettings.version) {
      // We're out of date.
      throw new MetaGlobalException.MetaGlobalStaleClientVersionException(serverSettings.version);
    }

    if (serverSettings.syncID == null) {
      // No syncID. This should never happen. Wipe the server.
      throw new MetaGlobalException.MetaGlobalMalformedSyncIDException();
    }

    if (!serverSettings.syncID.equals(clientSettings.syncID)) {
      // Our syncID is wrong. Reset client and take the server syncID.
      throw new MetaGlobalException.MetaGlobalStaleClientSyncIDException(serverSettings.syncID);
    }
  }
}
