/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncStorageCollectionRequest;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.mozilla.gecko.sync.net.WBOCollectionRequestDelegate;

@Category(IntegrationTestCategory.class)
public class TestWBOCollectionRequestDelegate {
  public static final String LOG_TAG = "TestWBOCollReqDel";

  static final String REMOTE_BOOKMARKS_URL = "https://phx-sync545.services.mozilla.com/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/bookmarks?full=1";
  static final String USERNAME     = "c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd";
  static final String USER_PASS    = "c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd:password";
  static final String SYNC_KEY     = "6m8mv8ex2brqnrmsb9fjuvfg7y";

  public class LiveDelegate extends WBOCollectionRequestDelegate {

    public KeyBundle bookmarksBundle = null;
    public ArrayList<CryptoRecord> wbos = new ArrayList<CryptoRecord>();

    @Override
    public String credentials() {
      return USER_PASS;
    }

    @Override
    public AuthHeaderProvider getAuthHeaderProvider() {
      return null;
    }

    @Override
    public String ifUnmodifiedSince() {
      return null;
    }

    @Override
    public void handleRequestSuccess(SyncStorageResponse response) {
      try {
        Logger.debug(LOG_TAG, "WBOs: " + this.wbos.size());
        assertTrue(13 < wbos.size());
        for (CryptoRecord record : this.wbos) {
          try {
            // TODO: make this an actual test. Return data locally.
            CryptoRecord decrypted = (CryptoRecord)(record.decrypt());
            Logger.debug(LOG_TAG, decrypted.payload.toJSONString());
          } catch (Exception e) {
            e.printStackTrace();
            fail("Decryption failed.");
          }
        }
      } finally {
        BaseResource.consumeEntity(response);
      }
    }

    @Override
    public void handleRequestFailure(SyncStorageResponse response) {
      BaseResource.consumeEntity(response);
      fail("Should not fail.");
    }

    @Override
    public void handleRequestError(Exception ex) {
      if (ex instanceof IOException) {
        // Assume that this is because Jenkins doesn't have network access.
        return;
      }
      fail("Should not error.");
    }

    @Override
    public void handleWBO(CryptoRecord record) {
      this.wbos.add(record);
    }

    @Override
    public KeyBundle keyBundle() {
      return this.bookmarksBundle;
    }
  }

  @Test
  public void testRealLiveBookmarks() throws URISyntaxException, UnsupportedEncodingException {
    URI u = new URI(REMOTE_BOOKMARKS_URL);
    SyncStorageCollectionRequest r = new SyncStorageCollectionRequest(u);
    LiveDelegate delegate = new LiveDelegate();
    r.delegate = delegate;

    // Here are our keys.
    String encrKey   = "0A7mU5SZ/tu7ZqwXW1og4qHVHN+zgEi4Xwfwjw+vEJw=";
    String hmacKey   = "11GN34O9QWXkjR06g8t0gWE1sGgQeWL0qxxWwl8Dmxs=";
    delegate.bookmarksBundle = KeyBundle.fromBase64EncodedKeys(encrKey, hmacKey);

    r.get(); 
  }
}
