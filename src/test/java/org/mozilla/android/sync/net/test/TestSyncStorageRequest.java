/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.net.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;
import org.mozilla.android.sync.BaseCryptoRecord;
import org.mozilla.android.sync.ExtendedJSONObject;
import org.mozilla.android.sync.crypto.KeyBundle;
import org.mozilla.android.sync.net.SyncStorageRequest;
import org.mozilla.android.sync.net.SyncStorageRequestDelegate;
import org.mozilla.android.sync.net.SyncStorageResponse;

public class TestSyncStorageRequest {

  static String            STORAGE_URL  = "http://localhost:8080/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/meta/global";
  // Corresponds to rnewman+testandroid@mozilla.com.
  static String            USERNAME     = "c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd";
  static String            USER_PASS    = "c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd:password";
  static String            SYNC_KEY     = "6m8mv8ex2brqnrmsb9fjuvfg7y";
  static String            EXPECT_BASIC = "Basic YzZvN2R2bXIyYzR1ZDJmeXY2d296MnU0emkyMmJjeWQ6cGFzc3dvcmQ=";
  private HTTPServerTestHelper data         = new HTTPServerTestHelper();

  public class BaseTestStorageRequestDelegate implements
      SyncStorageRequestDelegate {
    public String _credentials;

    @Override
    public String credentials() {
      return _credentials;
    }

    @Override
    public String ifUnmodifiedSince() {
      return null;
    }

    @Override
    public void handleSuccess(SyncStorageResponse res) {
      fail("Should not be called.");
    }

    @Override
    public void handleFailure(SyncStorageResponse response) {
      System.out.println("Response: "
          + response.httpResponse().getStatusLine().getStatusCode());
      fail("Should not be called.");
    }

    @Override
    public void handleError(IOException e) {
      fail("Should not be called.");
    }
  }

  public class TestSyncStorageRequestDelegate extends
      BaseTestStorageRequestDelegate {
    @Override
    public void handleSuccess(SyncStorageResponse res) {
      assertTrue(res.wasSuccessful());
      assertEquals(res.reason(), SyncStorageResponse.Reason.SUCCESS);
      assertTrue(res.httpResponse().containsHeader("X-Weave-Timestamp"));
      data.stopHTTPServer();
    }
  }

  public class LiveDelegate extends BaseTestStorageRequestDelegate {
    public boolean shouldDecrypt = false;

    @Override
    public void handleSuccess(SyncStorageResponse res) {
      assertTrue(res.wasSuccessful());
      assertEquals(res.reason(), SyncStorageResponse.Reason.SUCCESS);
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
          BaseCryptoRecord rec;
          //System.out.println("Body: " + res.body());
          ExtendedJSONObject body = (ExtendedJSONObject) res.jsonBody();
          rec = new BaseCryptoRecord(body);
          rec.keyBundle = new KeyBundle(USERNAME, SYNC_KEY);
          rec.decrypt();
          System.out.println(rec.cleartext.toJSONString());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  @Test
  public void testSyncStorageRequest() throws URISyntaxException, IOException {
    data.startHTTPServer();
    SyncStorageRequest r = new SyncStorageRequest(new URI(STORAGE_URL));
    TestSyncStorageRequestDelegate delegate = new TestSyncStorageRequestDelegate();
    delegate._credentials = USER_PASS;
    r.delegate = delegate;
    r.get();
    // Server is stopped in the callback.
  }

  @Test
  public void testRealLiveMetaGlobal() throws URISyntaxException {
    URI u = new URI(
        "https://phx-sync545.services.mozilla.com/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/meta/global");
    SyncStorageRequest r = new SyncStorageRequest(u);
    LiveDelegate delegate = new LiveDelegate();
    delegate._credentials = USER_PASS;
    r.delegate = delegate;
    r.get();

  }

  @Test
  public void testRealLiveCryptoKeys() throws URISyntaxException {
    URI u = new URI(
        "https://phx-sync545.services.mozilla.com/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/crypto/keys");
    SyncStorageRequest r = new SyncStorageRequest(u);
    LiveDelegate delegate = new LiveDelegate();
    delegate.shouldDecrypt = true;
    delegate._credentials = USER_PASS;
    r.delegate = delegate;
    r.get();
  }

}
