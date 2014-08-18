/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.fxa.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mozilla.android.sync.test.integration.IntegrationTestCategory;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.background.fxa.oauth.FxAccountAbstractClient;
import org.mozilla.gecko.background.fxa.oauth.FxAccountAbstractClientException.FxAccountAbstractClientRemoteException;
import org.mozilla.gecko.background.fxa.oauth.FxAccountOAuthClient10.AuthorizationResponse;
import org.mozilla.gecko.background.fxa.test.FxAccountTestHelper.StableDevTestHelper;
import org.mozilla.gecko.background.testhelpers.WaitHelper;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.net.BaseResource;

@Category(IntegrationTestCategory.class)
public class TestLiveFxAccountOAuthClient10 {
  protected final FxAccountTestHelper helper = new StableDevTestHelper();

  @Before
  public void setUp() throws Exception {
    Logger.startLoggingToConsole();
    BaseResource.rewriteLocalhost = false;
  }

  @Test
  public void testAuthorization() throws Throwable {
    final AuthorizationResponse authorization = helper.doTestAuthorization("testtesto@mockmyid.com", "testtesto@mockmyid.com", "profile");
    final ExtendedJSONObject profile = doProfile(authorization.access_token);

    Assert.assertNotNull(profile);
    Assert.assertEquals("testtesto@mockmyid.com", profile.getString("email"));
  }

  public ExtendedJSONObject doProfile(final String token) {
    final ExtendedJSONObject[] results = new ExtendedJSONObject[1];

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        helper.profileClient.profile(token, new FxAccountAbstractClient.RequestDelegate<ExtendedJSONObject>() {
          @Override
          public void handleSuccess(ExtendedJSONObject result) {
            results[0] = result;
            WaitHelper.getTestWaiter().performNotify();
          }

          @Override
          public void handleFailure(FxAccountAbstractClientRemoteException e) {
            WaitHelper.getTestWaiter().performNotify(e);
          }

          @Override
          public void handleError(Exception e) {
            WaitHelper.getTestWaiter().performNotify(e);
          }
        });
      }
    });

    return results[0];
  }
}
