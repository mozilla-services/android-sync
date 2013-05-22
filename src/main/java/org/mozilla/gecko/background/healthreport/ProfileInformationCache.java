/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.healthreport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.background.healthreport.EnvironmentBuilder.ProfileInformationProvider;

/**
 * There are some parts of the FHR environment that can't be readily computed
 * without a running Gecko -- add-ons, for example. In order to make this
 * information available without launching Gecko, we persist it on Fennec
 * startup. This class is the notepad in which we write.
 *
 * TODO: add-ons.
 */
public class ProfileInformationCache implements ProfileInformationProvider {
  private static final String LOG_TAG = "GeckoProfileInfo";
  private static final String CACHE_FILE = "profile_info_cache.json";

  protected boolean initialized = false;
  protected boolean needsWrite = false;

  protected final File file;

  private volatile boolean blocklistEnabled = true;
  private volatile boolean telemetryEnabled = false;
  private volatile long profileCreationTime = 0;

  public ProfileInformationCache(String profilePath) {
    file = new File(profilePath + File.separator + CACHE_FILE);
    Logger.info(LOG_TAG, "Using " + file.getAbsolutePath() + " for profile information cache.");
  }

  public synchronized void beginInitialization() {
    initialized = false;
    needsWrite = true;
    profileCreationTime = 0;
  }

  public JSONObject toJSON() {
    JSONObject object = new JSONObject();
    try {
      object.put("blocklist", blocklistEnabled);
      object.put("telemetry", telemetryEnabled);
      object.put("profileCreated", profileCreationTime);
    } catch (JSONException e) {
      // There isn't much we can do about this.
      // Let's just quietly muffle.
      return null;
    }
    return object;
  }

  private void fromJSON(JSONObject object) throws JSONException {
    blocklistEnabled = object.getBoolean("blocklist");
    telemetryEnabled = object.getBoolean("telemetry");
    profileCreationTime = object.getLong("profileCreated");
  }

  /**
   * Call this <b>on a background thread</b> when you're done adding things.
   * @throws IOException if there was a problem serializing or writing the cache to disk.
   */
  public synchronized void completeInitialization() throws IOException {
    initialized = true;
    if (!needsWrite) {
      Logger.debug(LOG_TAG, "No write needed.");
      return;
    }

    JSONObject object = toJSON();
    if (object == null) {
      throw new IOException("Couldn't serialize JSON.");
    }

    Logger.info(LOG_TAG, "Writing profile information to " + file.getAbsolutePath());
    FileOutputStream stream = new FileOutputStream(file);
    OutputStreamWriter writer = new OutputStreamWriter(stream, Charset.forName("UTF-8"));
    try {
      writer.append(object.toString());
      needsWrite = false;
    } finally {
      writer.close();
    }
  }

  /**
   * Call this if you're interested in reading.
   *
   * You should be doing so on a background thread.
   *
   * @return true if this object was initialized correctly.
   */
  public synchronized boolean restoreUnlessInitialized() {
    if (initialized) {
      return true;
    }

    if (!file.exists()) {
      return false;
    }

    // One-liner for file reading in Java. So sorry.
    Logger.info(LOG_TAG, "Restoring ProfileInformationCache from " + file.getAbsolutePath());
    Scanner scanner = null;
    try {
      scanner = new Scanner(file, "UTF-8");
      final String contents = scanner.useDelimiter("\\A").next();
      fromJSON(new JSONObject(contents));
      initialized = true;
      return true;
    } catch (FileNotFoundException e) {
      return false;
    } catch (JSONException e) {
      return false;
    } finally {
      if (scanner != null) {
        scanner.close();
      }
    }
  }

  private void ensureInitialized() {
    if (!initialized) {
      throw new IllegalStateException("Not initialized.");
    }
  }

  @Override
  public boolean isBlocklistEnabled() {
    ensureInitialized();
    return blocklistEnabled;
  }

  public void setBlocklistEnabled(boolean value) {
    Logger.debug(LOG_TAG, "Setting blocklist enabled: " + value);
    blocklistEnabled = value;
    needsWrite = true;
  }

  @Override
  public boolean isTelemetryEnabled() {
    ensureInitialized();
    return telemetryEnabled;
  }

  public void setTelemetryEnabled(boolean value) {
    Logger.debug(LOG_TAG, "Setting telemetry enabled: " + value);
    telemetryEnabled = value;
    needsWrite = true;
  }

  @Override
  public long getProfileCreationTime() {
    ensureInitialized();
    return profileCreationTime;
  }

  public void setProfileCreationTime(long value) {
    Logger.debug(LOG_TAG, "Setting profile creation time: " + value);
    profileCreationTime = value;
    needsWrite = true;
  }
}
