/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.integration;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.BaseTestStorageRequestDelegate;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.crypto.CryptoException;
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
    protected String body = null;

    protected final String username;
    protected final String password;

    public LiveDelegate(String username, String password) {
      this.username = username;
      this.password = password;
      this._credentials = username + ":" + password;
    }

    @Override
    public void handleRequestSuccess(SyncStorageResponse res) {
      try {
        assertTrue(res.wasSuccessful());
        assertTrue(res.httpResponse().containsHeader("X-Weave-Timestamp"));
        body = res.body();
      } catch (Exception e) {
        WaitHelper.getTestWaiter().performNotify(e);
        return;
      }
      WaitHelper.getTestWaiter().performNotify();
    }

    public String body() {
      return body;
    }

    public ExtendedJSONObject jsonObject() throws NonObjectJSONException, IOException, ParseException {
      return ExtendedJSONObject.parseJSONObject(body);
    }

    public ExtendedJSONObject decrypt(String syncKey) throws NonObjectJSONException, ParseException, IOException, CryptoException {
      CryptoRecord rec;
      rec = CryptoRecord.fromJSONRecord(body);
      rec.keyBundle = new KeyBundle(username, syncKey);
      rec.decrypt();
      return rec.payload;
    }
  }

  public static LiveDelegate realLiveFetch(String username, String password, String url) throws URISyntaxException {
    final SyncStorageRecordRequest r = new SyncStorageRecordRequest(new URI(url));
    LiveDelegate delegate = new LiveDelegate(username, password);
    r.delegate = delegate;
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        r.get();
      }
    });
    return delegate;
  }

  public static LiveDelegate realLivePut(String username, String password, String url, final CryptoRecord record) throws URISyntaxException {
    final SyncStorageRecordRequest r = new SyncStorageRecordRequest(new URI(url));
    LiveDelegate delegate = new LiveDelegate(username, password);
    r.delegate = delegate;
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        r.put(record);
      }
    });
    return delegate;
  }

  @Test
  public void testRealLiveMetaGlobal() throws Exception {
    System.out.println(realLiveFetch(USERNAME, PASSWORD, REMOTE_META_URL).body());
  }

  @Test
  public void testRealLiveCryptoKeys() throws Exception {
    System.out.println(realLiveFetch(USERNAME, PASSWORD, REMOTE_KEYS_URL).decrypt(SYNC_KEY));
  }

  @Test
  public void testRealLiveInfoCollections() throws Exception {
    System.out.println(realLiveFetch(USERNAME, PASSWORD, REMOTE_INFO_COLLECTIONS_URL).body());
  }
}
