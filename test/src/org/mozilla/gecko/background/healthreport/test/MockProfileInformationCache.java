/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.healthreport.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.gecko.background.healthreport.ProfileInformationCache;

public class MockProfileInformationCache extends ProfileInformationCache {
  public MockProfileInformationCache(String profilePath) {
    super(profilePath);
  }

  public boolean isInitialized() {
    return this.initialized;
  }
  public boolean needsWrite() {
    return this.needsWrite;
  }
  public File getFile() {
    return this.file;
  }

  public void writeJSON(JSONObject toWrite) throws IOException {
    writeToFile(toWrite);
  }

  public JSONObject readJSON() throws FileNotFoundException, JSONException {
    return readFromFile();
  }
}