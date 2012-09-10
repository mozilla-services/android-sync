/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.Resource;
import org.mozilla.gecko.sync.net.SyncResourceDelegate;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;

public class MockResourceDelegate extends SyncResourceDelegate {
  public WaitHelper waitHelper = null;
  public static String USER_PASS    = "john:password";
  public static String EXPECT_BASIC = "Basic am9objpwYXNzd29yZA==";

  public boolean handledHttpResponse = false;
  public HttpResponse httpResponse = null;

  public MockResourceDelegate(WaitHelper waitHelper, final Resource resource) {
    super();
    this.waitHelper = waitHelper;
  }

  @Override
  public String getCredentials() {
    return USER_PASS;
  }

  @Override
  public void handleHttpProtocolException(ClientProtocolException e) {
    waitHelper.performNotify(e);
  }

  @Override
  public void handleHttpIOException(IOException e) {
    waitHelper.performNotify(e);
  }

  @Override
  public void handleTransportException(GeneralSecurityException e) {
    waitHelper.performNotify(e);
  }

  @Override
  public void handleHttpResponse(HttpResponse response) {
    handledHttpResponse = true;
    httpResponse = response;

    assertEquals(response.getStatusLine().getStatusCode(), 200);
    BaseResource.consumeEntity(response);
    waitHelper.performNotify();
  }
}
