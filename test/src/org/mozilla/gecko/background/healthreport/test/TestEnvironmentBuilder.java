/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.healthreport.test;

import java.io.File;
import java.io.IOException;

import org.mozilla.gecko.AppConstants;
import org.mozilla.gecko.background.healthreport.Environment;
import org.mozilla.gecko.background.healthreport.EnvironmentBuilder;
import org.mozilla.gecko.background.healthreport.HealthReportConstants;
import org.mozilla.gecko.background.test.helpers.FakeProfileTestCase;

public class TestEnvironmentBuilder extends FakeProfileTestCase {
  public void testSanity() throws IOException {
    File subdir = new File(this.fakeProfileDirectory.getAbsolutePath() +
                           File.separator + "testPersisting");
    subdir.mkdir();
    long now = System.currentTimeMillis();
    int expectedDays = (int) (now / HealthReportConstants.MILLISECONDS_PER_DAY);

    MockProfileInformationCache cache = new MockProfileInformationCache(subdir.getAbsolutePath());
    assertFalse(cache.getFile().exists());
    cache.beginInitialization();
    cache.setBlocklistEnabled(true);
    cache.setTelemetryEnabled(false);
    cache.setProfileCreationTime(now);
    cache.completeInitialization();
    assertTrue(cache.getFile().exists());

    Environment environment = EnvironmentBuilder.getCurrentEnvironment(cache);
    assertEquals(AppConstants.MOZ_APP_BUILDID, environment.appBuildID);
    assertEquals("Android", environment.os);
    assertTrue(100 < environment.memoryMB); // Seems like a sane lower bound...
    assertTrue(environment.cpuCount >= 1);
    assertEquals(1, environment.isBlocklistEnabled);
    assertEquals(0, environment.isTelemetryEnabled);
    assertEquals(expectedDays, environment.profileCreation);
    assertEquals(EnvironmentBuilder.getCurrentEnvironment(cache).getHash(),
                 environment.getHash());

    cache.beginInitialization();
    cache.setBlocklistEnabled(false);
    cache.completeInitialization();

    assertFalse(EnvironmentBuilder.getCurrentEnvironment(cache).getHash()
                                  .equals(environment.getHash()));
  }

  @Override
  protected String getCacheSuffix() {
    return System.currentTimeMillis() + Math.random() + ".foo";
  }
}
