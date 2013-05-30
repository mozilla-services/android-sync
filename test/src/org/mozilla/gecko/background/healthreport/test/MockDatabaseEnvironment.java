/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.healthreport.test;

import org.mozilla.gecko.background.healthreport.HealthReportDatabaseStorage;
import org.mozilla.gecko.background.healthreport.HealthReportDatabaseStorage.DatabaseEnvironment;

public class MockDatabaseEnvironment extends DatabaseEnvironment {
  public MockDatabaseEnvironment(HealthReportDatabaseStorage storage, Class<? extends EnvironmentAppender> appender) {
    super(storage, appender);
  }

  public MockDatabaseEnvironment(HealthReportDatabaseStorage storage) {
    super(storage);
  }

  public static class MockEnvironmentAppender extends EnvironmentAppender {
    public StringBuilder appended = new StringBuilder();

    public MockEnvironmentAppender() {
      super();
    }

    @Override
    public void append(String s) {
      appended.append(s);
    }

    @Override
    public void append(int v) {
      appended.append(v);
    }

    @Override
    public String toString() {
      return appended.toString();
    }
  }

  public MockDatabaseEnvironment mockInit(String version) {
    profileCreation = 1234;
    cpuCount        = 2;
    memoryMB        = 512;

    isBlocklistEnabled = 1;
    isTelemetryEnabled = 1;
    extensionCount     = 0;
    pluginCount        = 0;
    themeCount         = 0;

    architecture    = "";
    sysName         = "";
    sysVersion      = "";
    vendor          = "";
    appName         = "";
    appID           = "";
    appVersion      = version;
    appBuildID      = "";
    platformVersion = "";
    platformBuildID = "";
    os              = "";
    xpcomabi        = "";
    updateChannel   = "";

    return this;
  }
}
