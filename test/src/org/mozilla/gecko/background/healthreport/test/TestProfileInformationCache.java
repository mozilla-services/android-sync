/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.healthreport.test;

import java.io.File;
import java.io.IOException;

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

  public final void testPersisting() throws IOException {
    File subdir = new File(this.fakeProfileDirectory.getAbsolutePath() + File.separator + "testPersisting");
    subdir.mkdir();

    MockProfileInformationCache cache = new MockProfileInformationCache(subdir.getAbsolutePath());
    assertFalse(cache.getFile().exists());
    cache.beginInitialization();
    cache.setBlocklistEnabled(true);
    cache.setTelemetryEnabled(true);
    cache.setProfileCreationTime(1234L);
    cache.completeInitialization();
    assertTrue(cache.getFile().exists());

    cache = new MockProfileInformationCache(subdir.getAbsolutePath());
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

    cache = new MockProfileInformationCache(subdir.getAbsolutePath());
    assertFalse(cache.isInitialized());
    assertTrue(cache.restoreUnlessInitialized());

    assertTrue(cache.isInitialized());
    assertFalse(cache.isBlocklistEnabled());
    assertTrue(cache.isTelemetryEnabled());
    assertEquals(2345L, cache.getProfileCreationTime());
  }

  @Override
  protected String getCacheSuffix() {
    return System.currentTimeMillis() + Math.random() + ".foo";
  }
}
