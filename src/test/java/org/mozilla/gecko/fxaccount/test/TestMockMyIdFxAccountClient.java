package org.mozilla.gecko.fxaccount.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.fxaccount.FxAccountClientDelegate;
import org.mozilla.gecko.fxaccount.MockMyIdFxAccountClient;
import org.mozilla.gecko.sync.ExtendedJSONObject;

public class TestMockMyIdFxAccountClient extends MockMyIdFxAccountClient {
  @Test
  public void testGetAssertion() throws Exception {
    final ExtendedJSONObject wrapper = new ExtendedJSONObject();

    final MockMyIdFxAccountClient client = new MockMyIdFxAccountClient();

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.createAccount("foo@foo.com", "password", new FxAccountClientDelegate() {
          @Override
          public void onSuccess(ExtendedJSONObject o) {
            wrapper.put("wrapped", o);

            WaitHelper.getTestWaiter().performNotify();
          }

          @Override
          public void onFailure(Exception e) {
            WaitHelper.getTestWaiter().performNotify(e);
          }

          @Override
          public void onError(Exception e) {
            WaitHelper.getTestWaiter().performNotify(e);
          }
        });
      }
    });

    ExtendedJSONObject result = wrapper.getObject("wrapped");
    assertNotNull(result);
    String certificate = result.getString("certificate");
    assertNotNull(certificate);

    String assertion = client.getAssertion(result, "http://test.com");
    assertTrue(assertion.startsWith(certificate));
  }
}
