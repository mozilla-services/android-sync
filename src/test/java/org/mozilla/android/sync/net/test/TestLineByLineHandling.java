/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.net.test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.junit.Test;
import org.mozilla.android.sync.net.SyncStorageCollectionRequest;
import org.mozilla.android.sync.net.SyncStorageCollectionRequestDelegate;
import org.mozilla.android.sync.net.SyncStorageResponse;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

public class TestLineByLineHandling {
  static String                STORAGE_URL = "http://localhost:8080/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/lines";
  private HTTPServerTestHelper data        = new HTTPServerTestHelper();

  public ArrayList<String>     lines       = new ArrayList<String>();

  public class LineByLineMockServer extends MockServer {
    public void handle(Request request, Response response) {
      try {
        System.out.println("Handling line-by-line request...");
        PrintStream bodyStream = this.handleBasicHeaders(request, response, 200, "application/newlines");

        bodyStream.print("First line.\n");
        bodyStream.print("Second line.\n");
        bodyStream.print("Third line.\n");
        bodyStream.print("Fourth line.\n");
        bodyStream.close();
      } catch (IOException e) {
        System.err.println("Oops.");
      }
    }
  }

  public class BaseLineByLineDelegate extends
      SyncStorageCollectionRequestDelegate {

    @Override
    public void handleProgress(String progress) {
      lines.add(progress);
    }

    @Override
    public String credentials() {
      return null;
    }

    @Override
    public String ifUnmodifiedSince() {
      return null;
    }

    @Override
    public void handleSuccess(SyncStorageResponse res) {
      assertTrue(res.wasSuccessful());
      assertEquals(res.reason(), SyncStorageResponse.Reason.SUCCESS);
      assertTrue(res.httpResponse().containsHeader("X-Weave-Timestamp"));

      assertEquals(lines.size(), 4);
      assertEquals(lines.get(0), "First line.");
      assertEquals(lines.get(1), "Second line.");
      assertEquals(lines.get(2), "Third line.");
      assertEquals(lines.get(3), "Fourth line.");
      data.stopHTTPServer();
    }

    @Override
    public void handleFailure(SyncStorageResponse response) {
      fail("Should not be called.");
    }

    @Override
    public void handleError(Exception ex) {
      fail("Should not be called.");
    }
  }

  @Test
  public void testLineByLine() throws URISyntaxException {
    data.startHTTPServer(new LineByLineMockServer());
    SyncStorageCollectionRequest r = new SyncStorageCollectionRequest(new URI(STORAGE_URL));
    SyncStorageCollectionRequestDelegate delegate = new BaseLineByLineDelegate();
    r.delegate = delegate;
    r.get();
    // Server is stopped in the callback.
  }
}
