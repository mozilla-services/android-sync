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
    File subdir = new File(this.fakeProfileDirectory.getAbsolutePath() + File.separator + "testPersisting");
    subdir.mkdir();
    return new MockProfileInformationCache(subdir.getAbsolutePath());
  }

  public final void testPersisting() throws IOException {
    MockProfileInformationCache cache = makeCache("testPersisting");
    assertFalse(cache.getFile().exists());
    cache.beginInitialization();
    cache.setBlocklistEnabled(true);
    cache.setTelemetryEnabled(true);
    cache.setProfileCreationTime(1234L);
    cache.completeInitialization();
    assertTrue(cache.getFile().exists());

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
    cache.writeJSON(json);
    assertTrue(cache.restoreUnlessInitialized());
    cache.beginInitialization();     // So that we'll need to read again.
    json.put("version", 2);
    cache.writeJSON(json);
    assertFalse(cache.restoreUnlessInitialized());
  }

  public final void testImplicitV1() throws JSONException, IOException {
    MockProfileInformationCache cache = makeCache("testImplicitV1");

    // This is a v1 payload without a version number.
    final JSONObject json = new JSONObject();
    json.put("blocklist", true);
    json.put("telemetry", false);
    json.put("profileCreated", 1234567L);
    json.put("addons", new JSONObject());

    cache.writeJSON(json);
    cache = makeCache("testImplicitV1");
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
