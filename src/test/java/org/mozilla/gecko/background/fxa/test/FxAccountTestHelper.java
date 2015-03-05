/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.fxa.test;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Assert;
import org.mozilla.gecko.background.fxa.FxAccountClient20;
import org.mozilla.gecko.background.fxa.FxAccountClient20.LoginResponse;
import org.mozilla.gecko.background.fxa.FxAccountClientException.FxAccountClientRemoteException;
import org.mozilla.gecko.background.fxa.FxAccountUtils;
import org.mozilla.gecko.background.fxa.oauth.FxAccountAbstractClient.RequestDelegate;
import org.mozilla.gecko.background.fxa.oauth.FxAccountAbstractClientException.FxAccountAbstractClientRemoteException;
import org.mozilla.gecko.background.fxa.oauth.FxAccountOAuthClient10;
import org.mozilla.gecko.background.fxa.oauth.FxAccountOAuthClient10.AuthorizationResponse;
import org.mozilla.gecko.background.fxa.profile.FxAccountProfileClient10;
import org.mozilla.gecko.background.testhelpers.WaitHelper;
import org.mozilla.gecko.browserid.BrowserIDKeyPair;
import org.mozilla.gecko.browserid.JSONWebTokenUtils;
import org.mozilla.gecko.browserid.RSACryptoImplementation;

/**
 * Map Firefox Account servers to HTTP clients.
 */
public abstract class FxAccountTestHelper {
  public static class ProdTestHelper extends FxAccountTestHelper {
    public ProdTestHelper() {
      super(
          "https://api.accounts.firefox.com/v1",
          "https://oauth.accounts.firefox.com/v1",
          "https://profile.accounts.firefox.com/v1",
          "3332a18d142636cb" // canGrant = true.
          );
    }
  }

  public static class StableDevTestHelper extends FxAccountTestHelper {
    public StableDevTestHelper() {
      super(
          "https://stable.dev.lcip.org/auth/v1",
          "https://oauth-stable.dev.lcip.org/v1",
          "https://stable.dev.lcip.org/profile/v1",
          "3332a18d142636cb" // https://oauth-stable.dev.lcip.org, canGrant = true.
          );
    }
  }

  public static class LatestDevTestHelper extends FxAccountTestHelper {
    public LatestDevTestHelper() {
      super(
          "https://latest.dev.lcip.org/auth/v1",
          "https://oauth-latest.dev.lcip.org/v1",
          "https://latest.dev.lcip.org/profile/v1",
          "0fddc2b28f47c2d8" // https://oauth-latest.dev.lcip.org, canGrant = true.
          );
    }
  }

  public final String authServerUri;
  public final String oauthServerUri;
  public final String profileServerUri;
  public final String clientId;

  public final ExecutorService executor;
  public final FxAccountClient20 authClient;
  public final FxAccountOAuthClient10 oauthClient;
  public final FxAccountProfileClient10 profileClient;

  public FxAccountTestHelper(String authServerUri, String oauthServerUri, String profileServerUri, String clientId) {
    this.authServerUri = authServerUri;
    this.oauthServerUri = oauthServerUri;
    this.profileServerUri = profileServerUri;
    this.clientId = clientId;

    executor = Executors.newSingleThreadExecutor();
    oauthClient = new FxAccountOAuthClient10(oauthServerUri, executor);
    authClient = new FxAccountClient20(authServerUri, executor);
    profileClient = new FxAccountProfileClient10(profileServerUri, executor);
  }

  protected BrowserIDKeyPair keyPair;
  public BrowserIDKeyPair getKeyPair() throws NoSuchAlgorithmException {
    if (keyPair == null) {
      keyPair = RSACryptoImplementation.generateKeyPair(1024);
    }
    return keyPair;
  }

  protected AuthorizationResponse doAuthorization(final String client_id, final String scope, final String assertion) {
    final AuthorizationResponse[] results = new AuthorizationResponse[1];

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        oauthClient.authorization(client_id, assertion, null, scope, new RequestDelegate<FxAccountOAuthClient10.AuthorizationResponse>() {
          @Override
          public void handleSuccess(AuthorizationResponse result) {
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

  public AuthorizationResponse doTestAuthorization(final String email, final String password, final String scope) throws Throwable {
    final String audience = FxAccountUtils.getAudienceForURL(oauthServerUri);

    try {
      LoginResponse createResponse = TestLiveFxAccountClient20.createAccount(authClient, email, password, false, TestLiveFxAccountClient20.VerificationState.UNVERIFIED);
      Assert.assertNotNull(createResponse.uid);
      Assert.assertNotNull(createResponse.sessionToken);
    } catch (FxAccountClientRemoteException e) {
      if (!(e.isAccountAlreadyExists() || e.isTooManyRequests())) {
        throw e;
      }
    }

    LoginResponse loginResponse = TestLiveFxAccountClient20.login(authClient, email, password, false);
    byte[] sessionToken = loginResponse.sessionToken;

    final BrowserIDKeyPair keyPair = getKeyPair();
    String certificate = TestLiveFxAccountClient20.certificateSign(authClient, keyPair.getPublic().toJSONObject(), 24*60*60*1000, sessionToken);

    final long expiresAt = JSONWebTokenUtils.DEFAULT_FUTURE_EXPIRES_AT_IN_MILLISECONDS;
    final String assertion = JSONWebTokenUtils.createAssertion(keyPair.getPrivate(), certificate, audience, JSONWebTokenUtils.DEFAULT_ASSERTION_ISSUER, null, expiresAt);
    JSONWebTokenUtils.dumpAssertion(assertion);

    final AuthorizationResponse authorization = doAuthorization(clientId, scope, assertion);
    Assert.assertNotNull(authorization);
    Assert.assertNotNull(authorization.scope);
    Assert.assertNotNull(authorization.access_token);

    return authorization;
  }
}
