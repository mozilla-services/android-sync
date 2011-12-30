/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.net.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.gecko.sync.net.BaseResource;

import android.util.Log;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;

public class TestResource {

  static String            USER_PASS    = "john:password";
  static String            EXPECT_BASIC = "Basic am9objpwYXNzd29yZA==";
  private HTTPServerTestHelper data     = new HTTPServerTestHelper();

  class BaseTestResourceDelegate extends BaseResourceDelegate {
    @Override
    public String getCredentials() {
      return null;
    }

    @Override
    public void handleHttpResponse(HttpResponse response) {
      fail("Should not occur.");
    }

    @Override
    public void handleHttpProtocolException(ClientProtocolException e) {
      fail("Should not occur.");
    }

    @Override
    public void handleHttpIOException(IOException e) {
      fail("Should not occur.");
    }

    @Override
    public void handleTransportException(GeneralSecurityException e) {
      fail("Should not occur.");
    }
  }

  private class TrivialTestResourceDelegate extends BaseTestResourceDelegate {
    @Override
    public String getCredentials() {
      return USER_PASS;
    }

    @Override
    public void handleHttpResponse(HttpResponse response) {
      assertEquals(response.getStatusLine().getStatusCode(), 200);
      data.stopHTTPServer();
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

  @Test
  public void testTrivialFetch() throws URISyntaxException {
    MockServer server = data.startHTTPServer();
    server.expectedBasicAuthHeader = EXPECT_BASIC;
    BaseResource r = new BaseResource("http://localhost:8080/foo/bar");
    // Truism!
    assertNotNull(r);
    r.delegate = new TrivialTestResourceDelegate();
    r.get();
  }

}
