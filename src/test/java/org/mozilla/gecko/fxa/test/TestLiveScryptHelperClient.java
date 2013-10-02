/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.fxa.test;

import junit.framework.Assert;

import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.fxa.ScryptHelperClient;
import org.mozilla.gecko.fxa.ScryptHelperClient.ScryptHelperDelegate;
import org.mozilla.gecko.sync.HTTPFailureException;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

import ch.boye.httpclientandroidlib.HttpResponse;

public class TestLiveScryptHelperClient {
  public static final String TEST_SERVERURL = "http://scrypt.dev.lcip.org";

  protected static class TestScryptHelperDelegate implements ScryptHelperDelegate {
    public String output = null;

    @Override
    public void onSuccess(String output) {
      this.output = output;
      WaitHelper.getTestWaiter().performNotify();
    }

    @Override
    public void onFailure(HttpResponse response) {
      WaitHelper.getTestWaiter().performNotify(new HTTPFailureException(new SyncStorageResponse(response)));
    }

    @Override
    public void onError(Exception e) {
      WaitHelper.getTestWaiter().performNotify(e);
    }
  }

  protected String doScrypt(final String input) {
    final ScryptHelperClient client = new ScryptHelperClient(TEST_SERVERURL);
    final TestScryptHelperDelegate delegate = new TestScryptHelperDelegate();

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.scrypt(input, delegate);
      }
    });
    return delegate.output;
  }

  // @org.junit.Test
  public void testScrypt() throws Exception {
    // Test vector from https://wiki.mozilla.org/Identity/AttachedServices/KeyServerProtocol#stretch-KDF.
    Assert.assertEquals("5b82f146a64126923e4167a0350bb181feba61f63cb1714012b19cb0be0119c5",
               doScrypt("f84913e3d8e6d624689d0a3e9678ac8dcc79d2c2f3d9641488cd9d6ef6cd83dd"));

    // This should fail with an HTTP 400, since the input is not hex-encoded.
    try {
      doScrypt("!.#@");
      Assert.fail("Excepted HTTP 400 exception.");
    } catch (WaitHelper.InnerError e) {
      Assert.assertTrue(e.innerError instanceof HTTPFailureException);
      Assert.assertEquals(400, ((HTTPFailureException) e.innerError).response.getStatusCode());
    }
  }
}
