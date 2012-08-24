package org.mozilla.android.sync.net.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;
import org.mozilla.android.sync.test.helpers.HTTPServerTestHelper;
import org.mozilla.android.sync.test.helpers.MockServer;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncStorageRecordRequest;
import org.mozilla.gecko.sync.net.SyncStorageRequestDelegate;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import ch.boye.httpclientandroidlib.Header;

public class TestSyncStorageRequestHeaders {
  private static final int    TEST_PORT   = 15325;
  private static final String TEST_SERVER = "http://localhost:" + TEST_PORT;
  public static final String  TEST_URL = TEST_SERVER + "/test";

  private HTTPServerTestHelper data = new HTTPServerTestHelper(TEST_PORT);

  public class EchoHeadersMockServer extends MockServer {
    @Override
    public void handle(Request request, Response response) {
      for (String header : request.getNames()) {
        response.set(header, request.getValue(header));
      }

      super.handle(request, response);
    }
  }

  public void testModifiedHeaders(final Long ifModifiedSince, final Long ifUnmodifiedSince) throws URISyntaxException {
    BaseResource.rewriteLocalhost = false;

    final SyncStorageRecordRequest r = new SyncStorageRecordRequest(new URI(TEST_URL));
    final SyncStorageRequestDelegate delegate = new SyncStorageRequestDelegate() {
      @Override
      public Long ifUnmodifiedSince() {
        return ifUnmodifiedSince;
      }

      @Override
      public Long ifModifiedSince() {
        return ifModifiedSince;
      }

      @Override
      public void handleRequestSuccess(SyncStorageResponse res) {
        final Header hIfModifiedSince = res.httpResponse().getLastHeader("X-If-Modified-Since");
        if (hIfModifiedSince == null) {
          assertNull(ifModifiedSince);
        } else if (ifModifiedSince == null) {
          assertNull(hIfModifiedSince);
        } else {
          assertEquals(ifModifiedSince.longValue(), Utils.decimalSecondsToMilliseconds(hIfModifiedSince.getValue()));
        }

        final Header hIfUnmodifiedSince = res.httpResponse().getLastHeader("X-If-Unmodified-Since");
        if (hIfUnmodifiedSince == null) {
          assertNull(ifUnmodifiedSince);
        } else if (ifUnmodifiedSince == null) {
          assertNull(hIfUnmodifiedSince);
        } else {
          assertEquals(ifUnmodifiedSince.longValue(), Utils.decimalSecondsToMilliseconds(hIfUnmodifiedSince.getValue()));
        }
        WaitHelper.getTestWaiter().performNotify();
      }

      @Override
      public String credentials() {
        return null;
      }

      @Override
      public void handleRequestFailure(SyncStorageResponse response) {
        WaitHelper.getTestWaiter().performNotify(new RuntimeException());
      }

      @Override
      public void handleRequestError(Exception ex) {
        WaitHelper.getTestWaiter().performNotify(ex);
      }
    };
    r.delegate = delegate;

    try {
      data.startHTTPServer(new EchoHeadersMockServer());
      WaitHelper.getTestWaiter().performWait(new Runnable() {
        @Override
        public void run() {
          r.get();
        }
      });
    } finally {
      data.stopHTTPServer();
    }
  }

  @Test
  public void testModifiedHeaders() throws URISyntaxException {
    testModifiedHeaders(null, null);
    testModifiedHeaders(Long.valueOf(1), null);
    testModifiedHeaders(null, Long.valueOf(2));
    testModifiedHeaders(Long.valueOf(1), Long.valueOf(2));
  }
}
