/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.net.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.json.simple.JSONObject;
import org.junit.Test;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncStorageRecordRequest;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

public class TestSyncStorageRequest {

  static final String REMOTE_META_URL = "https://phx-sync545.services.mozilla.com/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/meta/global";
  static final String REMOTE_KEYS_URL = "https://phx-sync545.services.mozilla.com/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/crypto/keys";
  static final String LOCAL_META_URL  = "http://localhost:8080/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/meta/global";
  static final String LOCAL_BAD_REQUEST_URL  = "http://localhost:8080/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/bad";

  static final String EXPECTED_ERROR_CODE = "12";

  // Corresponds to rnewman+testandroid@mozilla.com.
  static final String USERNAME     = "c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd";
  static final String USER_PASS    = "c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd:password";
  static final String SYNC_KEY     = "6m8mv8ex2brqnrmsb9fjuvfg7y";
  static final String EXPECT_BASIC = "Basic YzZvN2R2bXIyYzR1ZDJmeXY2d296MnU0emkyMmJjeWQ6cGFzc3dvcmQ=";

  private HTTPServerTestHelper data = new HTTPServerTestHelper();

  public class TestSyncStorageRequestDelegate extends
      BaseTestStorageRequestDelegate {
    @Override
    public void handleRequestSuccess(SyncStorageResponse res) {
      assertTrue(res.wasSuccessful());
      assertTrue(res.httpResponse().containsHeader("X-Weave-Timestamp"));
      data.stopHTTPServer();
    }
  }

  public class TestBadSyncStorageRequestDelegate extends
      BaseTestStorageRequestDelegate {

    @Override
    public void handleRequestFailure(SyncStorageResponse res) {
      assertTrue(!res.wasSuccessful());
      assertTrue(res.httpResponse().containsHeader("X-Weave-Timestamp"));
      try {
        String responseMessage = res.getErrorMessage();
        String expectedMessage = SyncStorageResponse.SERVER_ERROR_MESSAGES.get(EXPECTED_ERROR_CODE);
        assertEquals(expectedMessage, responseMessage);
      } catch (Exception e) {
        fail("Got exception fetching error message.");
      }
      data.stopHTTPServer();
    }
  }

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

  @Test
  public void testSyncStorageRequest() throws URISyntaxException, IOException {
    BaseResource.rewriteLocalhost = false;
    data.startHTTPServer();
    SyncStorageRecordRequest r = new SyncStorageRecordRequest(new URI(LOCAL_META_URL));
    TestSyncStorageRequestDelegate delegate = new TestSyncStorageRequestDelegate();
    delegate._credentials = USER_PASS;
    r.delegate = delegate;
    r.get();
    // Server is stopped in the callback.
  }

  public class ErrorMockServer extends MockServer {
    @Override
    public void handle(Request request, Response response) {
      super.handle(request, response, 400, EXPECTED_ERROR_CODE);
    }
  }

  @Test
  public void testErrorResponse() throws URISyntaxException {
    BaseResource.rewriteLocalhost = false;
    data.startHTTPServer(new ErrorMockServer());
    SyncStorageRecordRequest r = new SyncStorageRecordRequest(new URI(LOCAL_BAD_REQUEST_URL));
    TestBadSyncStorageRequestDelegate delegate = new TestBadSyncStorageRequestDelegate();
    delegate._credentials = USER_PASS;
    r.delegate = delegate;
    r.post(new JSONObject());
    // Server is stopped in the callback.
  }
}
