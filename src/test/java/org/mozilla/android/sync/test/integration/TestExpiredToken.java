/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.integration;

import java.net.URI;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mozilla.android.sync.test.integration.TestBasicFetch.LiveDelegate;
import org.mozilla.gecko.background.fxa.SkewHandler;
import org.mozilla.gecko.background.testhelpers.MockSharedPreferences;
import org.mozilla.gecko.background.testhelpers.WaitHelper;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.HawkAuthHeaderProvider;

import android.content.SharedPreferences;

@Category(IntegrationTestCategory.class)
public class TestExpiredToken extends TestWithTokenHelper {
  protected KeyBundle syncKeyBundle;
  protected SharedPreferences sharedPrefs;
  protected SyncConfiguration config;

  @Override
  protected String getMockMyIDUserName() {
    return "testExpiredTokens";
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    sharedPrefs = new MockSharedPreferences();
    syncKeyBundle = KeyBundle.withRandomKeys();
    config = new SyncConfiguration(getMockMyIDUserName(), authHeaderProvider, sharedPrefs, syncKeyBundle);
    config.clusterURL = new URI(token.endpoint);
  }

  @Test
  public void testGoodToken() throws Exception {
    // A good token should be accepted.
    final String url = config.infoCollectionsURL();
    final LiveDelegate ld = TestBasicFetch.realLiveFetch(authHeaderProvider, url);

    Assert.assertNotNull(ld.body());
  }

  @Test
  public void testBadToken() throws Exception {
    // A bad token should be rejected with a 401.
    final String id = token.id;
    final byte[] randomKey = Utils.generateRandomBytes(8);
    final AuthHeaderProvider badAuthHeaderProvider = new HawkAuthHeaderProvider(id, randomKey, false);

    try {
      final String url = config.infoCollectionsURL();
      TestBasicFetch.realLiveFetch(badAuthHeaderProvider, url);
    } catch (WaitHelper.InnerError e) {
      Assert.assertTrue(e.innerError instanceof RuntimeException);
      Assert.assertTrue(e.innerError.getMessage().contains("status code 401"));
    }
  }

  @Test
  public void testExpiredToken() throws Exception {
    // An expired token should be accepted.
    final SkewHandler tokenServerSkewHandler = SkewHandler.getSkewHandlerFromEndpointString(token.endpoint);
    final long tokenServerSkew = tokenServerSkewHandler.getSkewInSeconds();

    // A token collected long, long ago. This token is tied to the stage server
    // and the email address "testExpiredTokens@mockmyid.com".
    final String id = "eyJub2RlIjogImh0dHBzOi8vc3luYy0xLXVzLWVhc3QtMS5zdGFnZS5tb3phd3MubmV0IiwgImV4cGlyZXMiOiAxMzk1MTc2NjE3LjM3NTY0MTEsICJzYWx0IjogIjlhZDllMiIsICJ1aWQiOiAxODEzOTYzfeEEK0GGBwz2yAoju_zR4SB4GkYaHY2icbSebM62kLOP";
    final byte[] key = "E7wvCftqFrYvhmow2OVNiA8PycfGBZEBni9JTnihvY4=".getBytes("UTF-8");
    final AuthHeaderProvider expiredAuthHeaderProvider = new HawkAuthHeaderProvider(id, key, false, tokenServerSkew);

    final String url = config.infoCollectionsURL();
    final LiveDelegate ld = TestBasicFetch.realLiveFetch(expiredAuthHeaderProvider, url);

    Assert.assertNotNull(ld.body());
  }
}
