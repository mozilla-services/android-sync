/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.browserid.verifier.test;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;

import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.browserid.verifier.BrowserIDRemoteVerifierClient;
import org.mozilla.gecko.browserid.verifier.BrowserIDVerifierDelegate;
import org.mozilla.gecko.sync.ExtendedJSONObject;

public class BlockingBrowserIDVerifierClient {
  protected final BrowserIDRemoteVerifierClient verifierClient;

  public BlockingBrowserIDVerifierClient(URI verifierUri) {
    verifierClient = new BrowserIDRemoteVerifierClient(verifierUri);
  }

  public BlockingBrowserIDVerifierClient() throws URISyntaxException {
    verifierClient = new BrowserIDRemoteVerifierClient();
  }

  public void assertVerifySuccess(final String audience, final String assertion)
      throws URISyntaxException {
    final BrowserIDVerifierDelegate delegate = new BrowserIDVerifierDelegate() {
      @Override
      public void handleSuccess(ExtendedJSONObject response) {
        try {
          assertEquals(audience, response.getString("audience"));

          WaitHelper.getTestWaiter().performNotify();
        } catch (Throwable e) {
          WaitHelper.getTestWaiter().performNotify(e);
        }
      }

      @Override
      public void handleFailure(String reason) {
        WaitHelper.getTestWaiter().performNotify(new Exception(reason));
      }

      @Override
      public void handleError(Exception e) {
        WaitHelper.getTestWaiter().performNotify(e);
      }
    };

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        verifierClient.verify(audience, assertion, delegate);
      }
    });
  }

  public void assertVerifyFailure(final String audience, final String assertion,
      final String expectedReason)
      throws URISyntaxException {
    final BrowserIDVerifierDelegate delegate = new BrowserIDVerifierDelegate() {
      @Override
      public void handleSuccess(ExtendedJSONObject response) {
        WaitHelper.getTestWaiter().performNotify(new Exception("Expected failure."));
      }

      @Override
      public void handleFailure(String reason) {
        try {
          if (expectedReason != null) {
            assertEquals(expectedReason, reason);
          }

          WaitHelper.getTestWaiter().performNotify();
        } catch (Throwable e) {
          WaitHelper.getTestWaiter().performNotify(e);
        }
      }

      @Override
      public void handleError(Exception e) {
        WaitHelper.getTestWaiter().performNotify(e);
      }
    };

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        verifierClient.verify(audience, assertion, delegate);
      }
    });
  }
}
