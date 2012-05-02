/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.integration;

import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;
import org.mozilla.android.sync.test.helpers.BaseTestStorageRequestDelegate;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.net.SyncStorageRecordRequest;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

public class TestBasicFetch {
  // TODO: switch these to be the local server, with appropriate setup.
  static final String REMOTE_BOOKMARKS_URL        = "https://phx-sync545.services.mozilla.com/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/bookmarks?full=1";
  static final String REMOTE_META_URL             = "https://phx-sync545.services.mozilla.com/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/meta/global";
  static final String REMOTE_KEYS_URL             = "https://phx-sync545.services.mozilla.com/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/crypto/keys";
  static final String REMOTE_INFO_COLLECTIONS_URL = "https://phx-sync545.services.mozilla.com/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/info/collections";

  // Corresponds to rnewman+testandroid@mozilla.com.
  static final String USERNAME     = "c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd";
  static final String PASSWORD     = "password";
  static final String SYNC_KEY     = "6m8mv8ex2brqnrmsb9fjuvfg7y";

  public static class LiveDelegate extends BaseTestStorageRequestDelegate {
    public boolean shouldDecrypt = false;
    public String jsonBody = null;

    final String username;
    final String syncKey;

    public LiveDelegate(String username, String syncKey) {
      this.username = username;
      this.syncKey = syncKey;
    }

    @Override
    public void handleRequestSuccess(SyncStorageResponse res) {
      try {
        assertTrue(res.wasSuccessful());
        assertTrue(res.httpResponse().containsHeader("X-Weave-Timestamp"));
      } catch (Exception e) {
        WaitHelper.getTestWaiter().performNotify(e);
        return;
      }

      ExtendedJSONObject body = null;
      try {
        body = res.jsonObjectBody();
        jsonBody = body.toJSONString();
      } catch (Exception e) {
        WaitHelper.getTestWaiter().performNotify(e);
        return;
      }

      if (shouldDecrypt) {
        try {
          CryptoRecord rec;
          rec = CryptoRecord.fromJSONRecord(body);
          rec.keyBundle = new KeyBundle(username, syncKey);
          rec.decrypt();
          jsonBody = rec.payload.toJSONString();
        } catch (Exception e) {
          WaitHelper.getTestWaiter().performNotify(e);
          return;
        }
      }

      WaitHelper.getTestWaiter().performNotify();
    }
  }

  public static String realLiveFetch(String username, String password, String syncKey, String url, boolean shouldDecrypt) throws URISyntaxException {
    final SyncStorageRecordRequest r = new SyncStorageRecordRequest(new URI(url));
    LiveDelegate delegate = new LiveDelegate(username, syncKey);
    delegate.shouldDecrypt = shouldDecrypt;
    delegate._credentials = username + ":" + password;
    r.delegate = delegate;
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        r.get();
      }
    });
    return delegate.jsonBody;
  }

  @Test
  public void testRealLiveMetaGlobal() throws URISyntaxException {
    System.out.println(realLiveFetch(USERNAME, PASSWORD, SYNC_KEY, REMOTE_META_URL, false));
  }

  @Test
  public void testRealLiveCryptoKeys() throws URISyntaxException {
    System.out.println(realLiveFetch(USERNAME, PASSWORD, SYNC_KEY, REMOTE_KEYS_URL, true));
  }

  @Test
  public void testRealLiveInfoCollections() throws URISyntaxException {
    System.out.println(realLiveFetch(USERNAME, PASSWORD, SYNC_KEY, REMOTE_INFO_COLLECTIONS_URL, false));
  }
}
