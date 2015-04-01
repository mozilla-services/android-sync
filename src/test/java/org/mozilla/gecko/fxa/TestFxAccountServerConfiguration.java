/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxa;

import junit.framework.Assert;

import org.junit.Test;

public class TestFxAccountServerConfiguration {
  public final String defaultAuth = FxAccountConstants.DEFAULT_AUTH_SERVER_ENDPOINT;
  public final String defaultSync = FxAccountConstants.DEFAULT_TOKEN_SERVER_ENDPOINT;
  public final String defaultOAuth = FxAccountConstants.DEFAULT_OAUTH_SERVER_ENDPOINT;
  public final String defaultReading = FxAccountConstants.DEFAULT_READING_LIST_SERVER_ENDPOINT;

  public final String stageAuth = FxAccountConstants.STAGE_AUTH_SERVER_ENDPOINT;
  public final String stageSync = FxAccountConstants.STAGE_TOKEN_SERVER_ENDPOINT;
  public final String stageOAuth = FxAccountConstants.STAGE_OAUTH_SERVER_ENDPOINT;
  public final String stageReading = FxAccountConstants.STAGE_READING_LIST_SERVER_ENDPOINT;

  public final String unknown = "https://unknown.com/endpoint";

  protected void assertOthers(String oauth, String reading, FxAccountServerConfiguration config) {
    Assert.assertEquals(oauth, config.oauthServerEndpoint);
    Assert.assertEquals(reading, config.readingListServerEndpoint);
  }

  @Test
  public void testDefaults() throws Throwable {
    // Default configuration gets OAuth and Reading List.
    assertOthers(defaultOAuth, defaultReading, FxAccountServerConfiguration.withDefaultsFrom(defaultAuth, defaultSync));
    // Unusual configuration gets OAuth -- maybe they'll get a Profile service to go with it later?
    assertOthers(defaultOAuth, null, FxAccountServerConfiguration.withDefaultsFrom(defaultAuth, stageSync));

    // Stage configuration gets stage OAuth and stage Reading List.
    assertOthers(stageOAuth, stageReading, FxAccountServerConfiguration.withDefaultsFrom(stageAuth, stageSync));
    // Unusual stage configuration gets stage OAuth -- maybe they'll get a stage Profile...
    assertOthers(stageOAuth, null, FxAccountServerConfiguration.withDefaultsFrom(stageAuth, unknown));

    // Others get no OAuth or Reading List support for now.
    assertOthers(null, null, FxAccountServerConfiguration.withDefaultsFrom(unknown, unknown));
  }
}
