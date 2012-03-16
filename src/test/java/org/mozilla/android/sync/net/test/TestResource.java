/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.net.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.BaseResourceDelegate;
import org.mozilla.android.sync.test.helpers.HTTPServerTestHelper;
import org.mozilla.android.sync.test.helpers.MockServer;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.HttpResponseObserver;

import android.util.Log;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;

public class TestResource {
  private static final int    TEST_PORT   = 15325;
  private static final String TEST_SERVER = "http://localhost:" + TEST_PORT;

  static String            USER_PASS    = "john:password";
  static String            EXPECT_BASIC = "Basic am9objpwYXNzd29yZA==";
  private HTTPServerTestHelper data     = new HTTPServerTestHelper(TEST_PORT);

  class BaseTestResourceDelegate extends BaseResourceDelegate {
    @Override
    public String getCredentials() {
      return null;
    }

    @Override
    public void handleHttpResponse(HttpResponse response) {
      BaseResource.consumeEntity(response);
      try {
        fail("Should not happen.");
      } catch (Exception e) {
        WaitHelper.getTestWaiter().performNotify(e);
      }
    }

    @Override
    public void handleHttpProtocolException(ClientProtocolException e) {
      WaitHelper.getTestWaiter().performNotify(e);
    }

    @Override
    public void handleHttpIOException(IOException e) {
      WaitHelper.getTestWaiter().performNotify(e);
    }

    @Override
    public void handleTransportException(GeneralSecurityException e) {
      WaitHelper.getTestWaiter().performNotify(e);
    }
  }

  private class TrivialTestResourceDelegate extends BaseTestResourceDelegate {
    public boolean handledHttpResponse = false;

    @Override
    public String getCredentials() {
      return USER_PASS;
    }

    @Override
    public void handleHttpResponse(HttpResponse response) {
      handledHttpResponse = true;
      assertEquals(response.getStatusLine().getStatusCode(), 200);
      BaseResource.consumeEntity(response);
      WaitHelper.getTestWaiter().performNotify();
    }
  }

  @Before
  public void setUp() {
    Log.i("TestResource", "Faking SSL context.");
    BaseResource.enablePlainHTTPConnectionManager();
    Log.i("TestResource", "Disabling URI rewriting.");
    BaseResource.rewriteLocalhost = false;
  }

  @Test
  public void testLocalhostRewriting() throws URISyntaxException {
    BaseResource r = new BaseResource("http://localhost:5000/foo/bar", true);
    assertEquals("http://10.0.2.2:5000/foo/bar", r.getURI().toASCIIString());
  }

  public TrivialTestResourceDelegate doGet() throws URISyntaxException {
    final BaseResource r = new BaseResource(TEST_SERVER + "/foo/bar");
    TrivialTestResourceDelegate delegate = new TrivialTestResourceDelegate();
    r.delegate = delegate;
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        r.get();
      }
    });
    return delegate;
  }

  @Test
  public void testTrivialFetch() throws URISyntaxException {
    MockServer server = data.startHTTPServer();
    server.expectedBasicAuthHeader = EXPECT_BASIC;
    TrivialTestResourceDelegate delegate = doGet();
    assertTrue(delegate.handledHttpResponse);
    data.stopHTTPServer();
  }

  public static class MockHttpResponseObserver implements HttpResponseObserver {
    public HttpResponse response = null;

    @Override
    public void observeHttpResponse(HttpResponse response) {
      this.response = response;
    }
  }

  @Test
  public void testObserver() throws URISyntaxException {
    data.startHTTPServer();
    // Check that null observer doesn't fail.
    BaseResource.setHttpResponseObserver(null);
    doGet(); // HTTP server stopped in callback.

    // Check that non-null observer gets called with reasonable HttpResponse.
    MockHttpResponseObserver observer = new MockHttpResponseObserver();
    BaseResource.setHttpResponseObserver(observer);
    assertSame(observer, BaseResource.getHttpResponseObserver());
    assertNull(observer.response);
    doGet(); // HTTP server stopped in callback.
    assertNotNull(observer.response);
    assertEquals(200, observer.response.getStatusLine().getStatusCode());
    data.stopHTTPServer();
  }
}
