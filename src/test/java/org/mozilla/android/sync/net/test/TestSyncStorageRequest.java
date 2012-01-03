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
import org.mozilla.android.sync.test.helpers.BaseTestStorageRequestDelegate;
import org.mozilla.android.sync.test.helpers.HTTPServerTestHelper;
import org.mozilla.android.sync.test.helpers.MockServer;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncStorageRecordRequest;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

public class TestSyncStorageRequest {

  private static final String LOCAL_META_URL  = "http://localhost:8080/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/meta/global";
  private static final String LOCAL_BAD_REQUEST_URL  = "http://localhost:8080/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/bad";

  private static final String EXPECTED_ERROR_CODE = "12";

  // Corresponds to rnewman+testandroid@mozilla.com.
  private static final String USER_PASS    = "c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd:password";

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
