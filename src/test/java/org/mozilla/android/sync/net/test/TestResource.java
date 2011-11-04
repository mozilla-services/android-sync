/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.net.test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;

import org.mozilla.android.sync.net.BaseResource;
import org.mozilla.android.sync.net.SyncStorageRequest;
import org.mozilla.android.sync.net.SyncStorageRequestDelegate;
import org.mozilla.android.sync.net.SyncStorageResponse;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.simpleframework.http.core.Container;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.io.PrintStream;

public class TestResource {

  static String      STORAGE_URL  = "http://localhost:8080/1.1/c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd/storage/meta/global";
  // Corresponds to rnewman+testandroid@mozilla.com.
  static String      USER_PASS    = "c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd:password";
  static String      EXPECT_BASIC = "Basic YzZvN2R2bXIyYzR1ZDJmeXY2d296MnU0emkyMmJjeWQ6cGFzc3dvcmQ=";

  private Connection connection;
  private TestServer server;

  public class TestServer implements Container {
    public void handle(Request request, Response response) {
      try {
        PrintStream body = response.getPrintStream();
        long time = System.currentTimeMillis();
        response.set("Content-Type", "application/json");
        response.set("Server", "HelloWorld/1.0 (Simple 4.0)");
        response.setDate("Date", time);
        response.setDate("Last-Modified", time);
        response.set("X-Weave-Timestamp", Long.toString(time));
        System.out.println("Auth header: " + request.getValue("Authorization"));
        assertEquals(request.getValue("Authorization"), EXPECT_BASIC);
        body.println("Hello World");
        body.close();
      } catch (IOException e) {
        System.err.println("Oops.");
      }
    }
  }

  private void startHTTPServer() {
    try {
      server = new TestServer();
      connection = new SocketConnection(server);
      SocketAddress address = new InetSocketAddress(8080);
      connection.connect(address);
    } catch (IOException ex) {
      System.err.println("Error starting test HTTP server.");
      fail(ex.toString());
    }
  }

  private void stopHTTPServer() {
    try {
      connection.close();
      server = null;
      connection = null;
    } catch (IOException ex) {
      System.err.println("Error stopping test HTTP server.");
      fail(ex.toString());
    }
  }

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

  private class BaseTestResourceDelegate extends BaseResourceDelegate {
    @Override
    public String getCredentials() {
      return null;
    }

    @Override
    public void handleResponse(HttpResponse response) {
      fail("Should not occur.");
    }

    @Override
    public void handleProtocolException(ClientProtocolException e) {
      fail("Should not occur.");
    }

    @Override
    public void handleIOException(IOException e) {
      fail("Should not occur.");
    }
  }

  private class TrivialTestResourceDelegate extends BaseTestResourceDelegate {
    @Override
    public String getCredentials() {
      return "john:password";
    }

    @Override
    public void handleResponse(HttpResponse response) {
      assertEquals(response.getStatusLine().getStatusCode(), 200);
    }
  }

  @Test
  public void testTrivialFetch() throws URISyntaxException {
    BaseResource r = new BaseResource("http://google.com/");
    // Truism!
    assertNotNull(r);
    r.delegate = new TrivialTestResourceDelegate();
    r.get();
  }

  public class TestSyncStorageRequestDelegate extends
      BaseTestStorageRequestDelegate {
    @Override
    public void handleSuccess(SyncStorageResponse res) {
      assertTrue(res.wasSuccessful());
      assertEquals(res.reason(), SyncStorageResponse.Reason.SUCCESS);
      assertTrue(res.httpResponse().containsHeader("X-Weave-Timestamp"));
      stopHTTPServer();
    }
  }

  @Test
  public void testSyncStorageRequest() throws URISyntaxException, IOException {
    startHTTPServer();
    SyncStorageRequest r = new SyncStorageRequest(new URI(STORAGE_URL));
    TestSyncStorageRequestDelegate delegate = new TestSyncStorageRequestDelegate();
    delegate._credentials = USER_PASS;
    r.delegate = delegate;
    r.get();
    // Server is stopped in the callback.
  }

  public class LiveDelegate extends BaseTestStorageRequestDelegate {
    @Override
    public void handleSuccess(SyncStorageResponse res) {
      assertTrue(res.wasSuccessful());
      assertEquals(res.reason(), SyncStorageResponse.Reason.SUCCESS);
      assertTrue(res.httpResponse().containsHeader("X-Weave-Timestamp"));
      try {
        System.out.println(res.httpResponse().getEntity().getContent()
            .toString());
      } catch (IllegalStateException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
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
}
