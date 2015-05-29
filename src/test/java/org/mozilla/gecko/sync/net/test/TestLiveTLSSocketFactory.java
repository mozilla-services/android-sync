/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.net.test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mozilla.android.sync.test.integration.IntegrationTestCategory;
import org.mozilla.gecko.background.testhelpers.WaitHelper;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;
import org.mozilla.gecko.sync.net.SyncResponse;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;

/**
 * Android Services code talks to several different HTTPS endpoints. Some of
 * these endpoints have different TLS ciphersuites enabled. We test available
 * ciphersuites by connecting to the Digicert and GeoTrust SSL test servers;
 * GeoTrust's test server accepts weaker ciphersuites than Digicert's.
 */
@Category(IntegrationTestCategory.class)
public class TestLiveTLSSocketFactory {
  protected static void testSSLConnection(String uri) throws URISyntaxException {
    final BaseResource r = new BaseResource(uri);
    r.delegate = new BaseResourceDelegate(r) {
      @Override
      public String getUserAgent() {
        return null;
      }

      @Override
      public void handleHttpResponse(HttpResponse response) {
        try {
          final SyncResponse res = new SyncResponse(response);
          Assert.assertEquals(200, res.getStatusCode());
          WaitHelper.getTestWaiter().performNotify();
        } catch (Throwable e) {
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
    };

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        r.getBlocking();
      }
    });
  }

  @Test
  public void testWeakerCiphersuitesAllowed() throws Exception {
    testSSLConnection("https://ssltest11.bbtest.net");
  }

  @Test
  public void testWeakerCiphersuitesNotAllowed() throws Exception {
    testSSLConnection("https://global-root.digicert.com");
  }
}
