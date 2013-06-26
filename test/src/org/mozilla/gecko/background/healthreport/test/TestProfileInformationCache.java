/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.healthreport.test;

import java.io.File;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.gecko.background.test.helpers.FakeProfileTestCase;

public class TestProfileInformationCache extends FakeProfileTestCase {

  public final void testInitState() throws IOException {
    MockProfileInformationCache cache = new MockProfileInformationCache(this.fakeProfileDirectory.getAbsolutePath());
    assertFalse(cache.isInitialized());
    assertFalse(cache.needsWrite());

    try {
      cache.isBlocklistEnabled();
      fail("Should throw fetching isBlocklistEnabled.");
    } catch (IllegalStateException e) {
      // Great!
    }

    cache.beginInitialization();
    assertFalse(cache.isInitialized());
    assertTrue(cache.needsWrite());

    try {
      cache.isBlocklistEnabled();
      fail("Should throw fetching isBlocklistEnabled.");
    } catch (IllegalStateException e) {
      // Great!
    }

    cache.completeInitialization();
    assertTrue(cache.isInitialized());
    assertFalse(cache.needsWrite());
  }

  public final MockProfileInformationCache makeCache(final String suffix) {
    File subdir = new File(this.fakeProfileDirectory.getAbsolutePath() + File.separator + suffix);
    subdir.mkdir();
    return new MockProfileInformationCache(subdir.getAbsolutePath());
  }

  public final void testPersisting() throws IOException {
    MockProfileInformationCache cache = makeCache("testPersisting");
    // We start with no file.
    assertFalse(cache.getFile().exists());

    // Partially populate. Note that this doesn't happen in live code, but
    // apparently we can end up with null add-ons JSON on disk, so this
    // reproduces that scenario.
    cache.beginInitialization();
    cache.setBlocklistEnabled(true);
    cache.setTelemetryEnabled(true);
    cache.setProfileCreationTime(1234L);
    cache.completeInitialization();

    assertTrue(cache.getFile().exists());

    // But reading this from disk won't work, because we were only partially
    // initialized. We want to start over.
    cache = makeCache("testPersisting");
    assertFalse(cache.isInitialized());
    assertFalse(cache.restoreUnlessInitialized());
    assertFalse(cache.isInitialized());

    // Now fully populate, and try again...
    cache.beginInitialization();
    cache.setBlocklistEnabled(true);
    cache.setTelemetryEnabled(true);
    cache.setProfileCreationTime(1234L);
    cache.setJSONForAddons(new JSONObject());
    cache.completeInitialization();
    assertTrue(cache.getFile().exists());

    // ... and this time we succeed.
    cache = makeCache("testPersisting");
    assertFalse(cache.isInitialized());
    assertTrue(cache.restoreUnlessInitialized());
    assertTrue(cache.isInitialized());
    assertTrue(cache.isBlocklistEnabled());
    assertTrue(cache.isTelemetryEnabled());
    assertEquals(1234L, cache.getProfileCreationTime());

    // Mutate.
    cache.beginInitialization();
    assertFalse(cache.isInitialized());
    cache.setBlocklistEnabled(false);
    cache.setProfileCreationTime(2345L);
    cache.completeInitialization();
    assertTrue(cache.isInitialized());

    cache = makeCache("testPersisting");
    assertFalse(cache.isInitialized());
    assertTrue(cache.restoreUnlessInitialized());

    assertTrue(cache.isInitialized());
    assertFalse(cache.isBlocklistEnabled());
    assertTrue(cache.isTelemetryEnabled());
    assertEquals(2345L, cache.getProfileCreationTime());
  }

  public final void testVersioning() throws JSONException, IOException {
    MockProfileInformationCache cache = makeCache("testVersioning");
    final JSONObject json = cache.toJSON();
    assertEquals(1, json.getInt("version"));

    // Initialize enough that we can re-load it.
    cache.beginInitialization();
    cache.setJSONForAddons(new JSONObject());
    cache.completeInitialization();
    cache.writeJSON(json);
    assertTrue(cache.restoreUnlessInitialized());
    cache.beginInitialization();     // So that we'll need to read again.
    json.put("version", 2);
    cache.writeJSON(json);

    // We can't restore a future version.
    assertFalse(cache.restoreUnlessInitialized());
  }

  public final void testImplicitV1() throws JSONException, IOException {
    MockProfileInformationCache cache = makeCache("testImplicitV1");

    // This is a broken v1 payload without a version number.
    final JSONObject jsonInvalid = new JSONObject();
    jsonInvalid.put("blocklist", true);
    jsonInvalid.put("telemetry", false);
    jsonInvalid.put("profileCreated", 1234567L);

    final JSONObject jsonValid = new JSONObject(jsonInvalid, new String[]{"blocklist", "telemetry", "profileCreated"});
    jsonValid.put("addons", new JSONObject());

    cache.writeJSON(jsonInvalid);
    assertFalse(cache.restoreUnlessInitialized());

    cache = makeCache("testImplicitV1");
    cache.writeJSON(jsonValid);
    assertTrue(cache.restoreUnlessInitialized());

    cache.beginInitialization();
    cache.setTelemetryEnabled(true);
    cache.completeInitialization();

    assertEquals(1, cache.readJSON().getInt("version"));
  }

  @Override
  protected String getCacheSuffix() {
    return System.currentTimeMillis() + Math.random() + ".foo";
  }
}
