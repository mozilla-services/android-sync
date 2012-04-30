/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync;

import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.sync.SyncConfiguration.ConfigurationBranch;
import org.mozilla.gecko.sync.repositories.RepositorySessionBundle;

import android.content.SharedPreferences.Editor;

public class SynchronizerConfiguration {
  private static final String LOG_TAG = "SynczrConfiguration";

  private static final String PREFS_SYNCID  = "syncID";
  private static final String PREFS_VERSION = "version";
  private static final String PREFS_LOCAL   = "local";
  private static final String PREFS_REMOTE  = "remote";
  private static final String PREFS_ENABLED = "enabled";

  public String syncID;
  public int version;
  public boolean enabled;
  public RepositorySessionBundle remoteBundle;
  public RepositorySessionBundle localBundle;

  public SynchronizerConfiguration(ConfigurationBranch config) throws NonObjectJSONException, IOException, ParseException {
    this.load(config);
  }

  public SynchronizerConfiguration(String syncID, int version, boolean enabled, RepositorySessionBundle remoteBundle, RepositorySessionBundle localBundle) {
    this.syncID = syncID;
    this.version = version;
    this.enabled = enabled;
    this.remoteBundle = remoteBundle;
    this.localBundle  = localBundle;
  }

  // This should get partly shuffled back into SyncConfiguration, I think.
  public void load(ConfigurationBranch config) throws NonObjectJSONException, IOException, ParseException {
    if (config == null) {
      throw new IllegalArgumentException("config cannot be null.");
    }
    syncID = config.getString(PREFS_SYNCID, null);
    version = config.getInt(PREFS_VERSION, 0);
    enabled = config.getBoolean(PREFS_ENABLED, true);
    String remoteJSON = config.getString(PREFS_REMOTE, null);
    String localJSON  = config.getString(PREFS_LOCAL,  null);
    RepositorySessionBundle rB = new RepositorySessionBundle(remoteJSON);
    RepositorySessionBundle lB = new RepositorySessionBundle(localJSON);
    if (remoteJSON == null) {
      rB.setTimestamp(0);
    }
    if (localJSON == null) {
      lB.setTimestamp(0);
    }
    remoteBundle = rB;
    localBundle  = lB;
    Logger.info(LOG_TAG, "Initialized SynchronizerConfiguration. remoteBundle: " + remoteBundle + ", localBundle: " + localBundle);
  }

  public void persist(ConfigurationBranch config) {
    if (config == null) {
      throw new IllegalArgumentException("config cannot be null.");
    }
    Editor editor = config.edit();
    editor.putString(PREFS_SYNCID, syncID);
    editor.putInt(PREFS_VERSION, version);
    editor.putBoolean(PREFS_ENABLED, enabled);
    String jsonRemote = remoteBundle.toJSONString();
    String jsonLocal  = localBundle.toJSONString();
    editor.putString(PREFS_REMOTE, jsonRemote);
    editor.putString(PREFS_LOCAL,  jsonLocal);

    // Synchronous.
    editor.commit();
  }
}
