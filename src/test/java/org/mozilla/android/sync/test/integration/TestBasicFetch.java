/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;
import org.mozilla.android.sync.net.test.BaseTestStorageRequestDelegate;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.net.SyncStorageRecordRequest;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

public class TestBasicFetch {
  // TODO: switch these to be the local server, with appropriate setup.
  static final String REMOTE_BOOKMARKS_URL = "https://phx-sync545.services.mozilla.com/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/bookmarks?full=1";
  static final String REMOTE_META_URL = "https://phx-sync545.services.mozilla.com/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/meta/global";
  static final String REMOTE_KEYS_URL = "https://phx-sync545.services.mozilla.com/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/crypto/keys";

  // Corresponds to rnewman+testandroid@mozilla.com.
  static final String USERNAME     = "c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd";
  static final String USER_PASS    = "c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd:password";
  static final String SYNC_KEY     = "6m8mv8ex2brqnrmsb9fjuvfg7y";

  public class LiveDelegate extends BaseTestStorageRequestDelegate {
    public boolean shouldDecrypt = false;

    @Override
    public void handleRequestSuccess(SyncStorageResponse res) {
      assertTrue(res.wasSuccessful());
      assertTrue(res.httpResponse().containsHeader("X-Weave-Timestamp"));
      try {
        System.out.println(res.httpResponse().getEntity().getContent()
            .toString());
      } catch (Exception e) {
        e.printStackTrace();
      }

      if (shouldDecrypt) {
        try {
          System.out.println("Attempting decrypt.");
          CryptoRecord rec;
          rec = CryptoRecord.fromJSONRecord(res.jsonObjectBody());
          rec.keyBundle = new KeyBundle(USERNAME, SYNC_KEY);
          rec.decrypt();
          System.out.println(rec.payload.toJSONString());
        } catch (Exception e) {
          e.printStackTrace();
          fail("Should receive no exception when decrypting.");
        }
      }
    }
  }

  @Test
  public void testRealLiveMetaGlobal() throws URISyntaxException {
    URI u = new URI(
        REMOTE_META_URL);
    SyncStorageRecordRequest r = new SyncStorageRecordRequest(u);
    LiveDelegate delegate = new LiveDelegate();
    delegate._credentials = USER_PASS;
    r.delegate = delegate;
    r.get();
  }

  @Test
  public void testRealLiveCryptoKeys() throws URISyntaxException {
    URI u = new URI(REMOTE_KEYS_URL);
    SyncStorageRecordRequest r = new SyncStorageRecordRequest(u);
    LiveDelegate delegate = new LiveDelegate();
    delegate.shouldDecrypt = true;
    delegate._credentials = USER_PASS;
    r.delegate = delegate;
    r.get();
  }
}
