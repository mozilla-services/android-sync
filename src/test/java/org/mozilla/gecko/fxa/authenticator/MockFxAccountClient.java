/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.fxa.authenticator;

import java.util.HashMap;
import java.util.Map;

import org.mozilla.gecko.background.fxa.FxAccountClient;
import org.mozilla.gecko.background.fxa.FxAccountClient10.RequestDelegate;
import org.mozilla.gecko.background.fxa.FxAccountClient10.StatusResponse;
import org.mozilla.gecko.background.fxa.FxAccountClient10.TwoKeys;
import org.mozilla.gecko.background.fxa.FxAccountClient20.LoginResponse;
import org.mozilla.gecko.browserid.MockMyIDTokenFactory;
import org.mozilla.gecko.browserid.RSACryptoImplementation;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Utils;

public class MockFxAccountClient implements FxAccountClient {
  protected static MockMyIDTokenFactory mockMyIdTokenFactory = new MockMyIDTokenFactory();

  public final Map<String, User> users = new HashMap<String, User>();
  public final Map<String, String> sessionTokens = new HashMap<String, String>();
  public final Map<String, String> sessionKeyFetchTokens = new HashMap<String, String>();

  public static class User {
    public final String email;
    public final String password;
    public final String uid;
    public boolean verified;

    public User(String email, String password) {
      this.email = email;
      this.password = password;
      this.uid = "uid/" + this.email;
      this.verified = false;
    }
  }

  public void addUser(String email, String password) {
    User user = new User(email, password);
    users.put(user.email, user);
  }

  ////  @Override
  ////  public void createAccount(byte[] emailUTF8, byte[] passwordUTF8, boolean preVerified, RequestDelegate<String> delegate) {
  ////    User user;
  ////    try {
  ////      user = new User(new String(emailUTF8, "UTF-8"), new String(passwordUTF8, "UTF-8"));
  ////    } catch (Exception e) {
  ////      delegate.handleError(e);
  ////      return;
  ////    }
  ////    if (users.containsKey(user.email)) {
  ////      delegate.handleError(new HTTPFailureException(new SyncStorageResponse(new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), 400, "Bad Request"))));
  ////      return;
  ////    }
  ////
  //  }

  @Override
  public void status(byte[] sessionToken, RequestDelegate<StatusResponse> requestDelegate) {
    String email = "testEmail";
    boolean verified = true;
    requestDelegate.handleSuccess(new StatusResponse(email, verified));
  }

  @Override
  public void loginAndGetKeys(byte[] emailUTF8, byte[] passwordUTF8, RequestDelegate<LoginResponse> requestDelegate) {
    // requestDelegate.handleError(new RuntimeException("GOTCHA"));
    byte[] sessionToken = Utils.generateRandomBytes(32);
    byte[] keyFetchToken = Utils.generateRandomBytes(32);
    requestDelegate.handleSuccess(new LoginResponse("serverURI", "uid", false, sessionToken, keyFetchToken));
  }

  @Override
  public void keys(byte[] keyFetchToken, RequestDelegate<TwoKeys> requestDelegate) {
    // requestDelegate.handleError(new RuntimeException("GOTCHA"));
    byte[] kA = Utils.generateRandomBytes(32);
    byte[] wrapkB = Utils.generateRandomBytes(32);
    requestDelegate.handleSuccess(new TwoKeys(kA, wrapkB));
  }

  @Override
  public void sign(byte[] sessionToken, ExtendedJSONObject publicKey, long certificateDurationInMilliseconds, RequestDelegate<String> requestDelegate) {
    try {
      String certificate = mockMyIdTokenFactory.createMockMyIDCertificate(RSACryptoImplementation.createPublicKey(publicKey), "test", System.currentTimeMillis(), certificateDurationInMilliseconds);
      requestDelegate.handleSuccess(certificate);
    } catch (Exception e) {
      requestDelegate.handleError(e);
    }
  }
}
