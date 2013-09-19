/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.integration;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mozilla.android.sync.test.helpers.BaseTestStorageRequestDelegate;
import org.mozilla.gecko.background.testhelpers.WaitHelper;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.BasicAuthHeaderProvider;
import org.mozilla.gecko.sync.net.SyncStorageRecordRequest;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

@Category(IntegrationTestCategory.class)
public class TestBasicFetch {
  // TODO: switch these to be the local server, with appropriate setup.
  static final String REMOTE_BOOKMARKS_URL        = "https://phx-sync545.services.mozilla.com/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/bookmarks?full=1";
  static final String REMOTE_META_URL             = "https://phx-sync545.services.mozilla.com/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/meta/global";
  static final String REMOTE_KEYS_URL             = "https://phx-sync545.services.mozilla.com/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/crypto/keys";
  static final String REMOTE_INFO_COLLECTIONS_URL = "https://phx-sync545.services.mozilla.com/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/info/collections";

  // Corresponds to rnewman+testandroid@mozilla.com.
  static final String TEST_USERNAME     = "c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd";
  static final String TEST_PASSWORD     = "password";
  static final String SYNC_KEY     = "6m8mv8ex2brqnrmsb9fjuvfg7y";

  public static class LiveDelegate extends BaseTestStorageRequestDelegate {
    public LiveDelegate(String username, String password) {
      super(new BasicAuthHeaderProvider(username, password));
    }

    public LiveDelegate(AuthHeaderProvider authHeaderProvider) {
      super(authHeaderProvider);
    }

    protected String body = null;

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

    @Override
    public void handleRequestFailure(SyncStorageResponse response) {
      BaseResource.consumeEntity(response);
      try {
        WaitHelper.getTestWaiter().performNotify(new RuntimeException(response.body()));
      } catch (Exception e) {
        WaitHelper.getTestWaiter().performNotify(e);
      }
    }

    @Override
    public void handleRequestError(Exception e) {
      WaitHelper.getTestWaiter().performNotify(e);
    }

    public String body() {
      return body;
    }

    public ExtendedJSONObject jsonObject() throws NonObjectJSONException, IOException, ParseException {
      return ExtendedJSONObject.parseJSONObject(body);
    }

    public ExtendedJSONObject decrypt(String syncKey) throws Exception {
      return decrypt(new KeyBundle(TEST_USERNAME, syncKey));
    }

    public ExtendedJSONObject decrypt(KeyBundle keyBundle) throws Exception {
      CryptoRecord rec;
      rec = CryptoRecord.fromJSONRecord(body);
      rec.keyBundle = keyBundle;
      rec.decrypt();
      return rec.payload;
    }
  }

  public static LiveDelegate realLiveFetch(String username, String password, String url) throws URISyntaxException {
    return realLiveFetch(new BasicAuthHeaderProvider(username, password), url);
  }

  public static LiveDelegate realLiveFetch(AuthHeaderProvider authHeaderProvider, String url) throws URISyntaxException {
    final SyncStorageRecordRequest r = new SyncStorageRecordRequest(new URI(url));
    LiveDelegate delegate = new LiveDelegate(authHeaderProvider);
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
    return realLivePut(new BasicAuthHeaderProvider(username, password), url, record);
  }

  public static LiveDelegate realLivePut(AuthHeaderProvider authHeaderProvider, String url, final CryptoRecord record) throws URISyntaxException {
    final SyncStorageRecordRequest r = new SyncStorageRecordRequest(new URI(url));
    LiveDelegate delegate = new LiveDelegate(authHeaderProvider);
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
    LiveDelegate ld = realLiveFetch(TEST_USERNAME, TEST_PASSWORD, REMOTE_META_URL);
    System.out.println(ld.body());
  }

  @Test
  public void testRealLiveCryptoKeys() throws Exception {
    LiveDelegate ld = realLiveFetch(TEST_USERNAME, TEST_PASSWORD, REMOTE_KEYS_URL);
    System.out.println(ld.decrypt(SYNC_KEY));
  }

  @Test
  public void testRealLiveInfoCollections() throws Exception {
    LiveDelegate ld = realLiveFetch(TEST_USERNAME, TEST_PASSWORD, REMOTE_INFO_COLLECTIONS_URL);
    System.out.println(ld.body());
  }
}
