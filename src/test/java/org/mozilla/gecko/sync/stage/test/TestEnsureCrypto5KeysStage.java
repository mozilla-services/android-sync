/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.stage.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.MockGlobalSession;
import org.mozilla.android.sync.test.helpers.MockGlobalSessionCallback;
import org.mozilla.android.sync.test.helpers.MockSharedPreferences;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.InfoCollections;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.PrefsSource;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.stage.EnsureCrypto5KeysStage;

import android.content.SharedPreferences;

public class TestEnsureCrypto5KeysStage extends EnsureCrypto5KeysStage implements PrefsSource {
  private static final int    TEST_PORT   = 15325;
  private static final String TEST_SERVER = "http://localhost:" + TEST_PORT;
  private static final String USERNAME  = "john";
  private static final String PASSWORD  = "password";
  private static final String SYNC_KEY  = "abcdeabcdeabcdeabcdeabcdea";

  MockSharedPreferences prefs = null;

  @Override
  public SharedPreferences getPrefs(String name, int mode) {
    if (prefs == null) {
      prefs = new MockSharedPreferences();
    }
    return prefs;
  }

  @Before
  public void setUp() {
    Logger.LOG_TO_STDOUT = true;
    prefs = null;
  }

  @Test
  public void testUpdateNeeded() throws NonObjectJSONException, IOException, ParseException, CryptoException, SyncConfigurationException, IllegalArgumentException {
    InfoCollections infoCollections = new InfoCollections(null, null);
    String json = "{\"history\":1.3319567131E9,\"bookmarks\":1.33195669592E9," +
                   "\"prefs\":1.33115408641E9,\"crypto\":1.32046063664E9,\"meta\":1.32046073744E9," +
                   "\"forms\":1.33136685374E9,\"clients\":1.3313667619E9,\"tabs\":1.33136685397E9}";
    ExtendedJSONObject record = ExtendedJSONObject.parseJSONObject(json);
    infoCollections.setFromRecord(record);

    final MockGlobalSessionCallback callback = new MockGlobalSessionCallback();
    final KeyBundle bundle = new KeyBundle(USERNAME, SYNC_KEY);
    session = new MockGlobalSession(TEST_SERVER, USERNAME, PASSWORD, bundle, callback);

    // Test with no local timestamp set.
    assertTrue(this.updateNeeded(infoCollections));

    // Test with local timestamp set in the past.
    session.config.persistedCryptoKeys().persistLastModified(Utils.decimalSecondsToMilliseconds(1.3E9));
    assertTrue(this.updateNeeded(infoCollections));

    // Test with local timestamp set in the future.
    session.config.persistedCryptoKeys().persistLastModified(Utils.decimalSecondsToMilliseconds(1.4E9));
    assertFalse(this.updateNeeded(infoCollections));

    // Test with no crypto collection.
    infoCollections.setFromRecord(ExtendedJSONObject.parseJSONObject( "{\"history\":1.3319567131E9}"));
    assertTrue(this.updateNeeded(infoCollections));
  }
}
