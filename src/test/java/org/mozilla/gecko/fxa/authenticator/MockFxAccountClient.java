/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.fxa.authenticator;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.mozilla.gecko.background.fxa.FxAccountClient;
import org.mozilla.gecko.background.fxa.FxAccountClient10.RequestDelegate;
import org.mozilla.gecko.background.fxa.FxAccountClient10.StatusResponse;
import org.mozilla.gecko.background.fxa.FxAccountClient10.TwoKeys;
import org.mozilla.gecko.background.fxa.FxAccountClient20.LoginResponse;
import org.mozilla.gecko.background.fxa.FxAccountClientException.FxAccountClientRemoteException;
import org.mozilla.gecko.browserid.MockMyIDTokenFactory;
import org.mozilla.gecko.browserid.RSACryptoImplementation;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Utils;

import ch.boye.httpclientandroidlib.ProtocolVersion;
import ch.boye.httpclientandroidlib.entity.StringEntity;
import ch.boye.httpclientandroidlib.message.BasicHttpResponse;

public class MockFxAccountClient implements FxAccountClient {
  protected static MockMyIDTokenFactory mockMyIdTokenFactory = new MockMyIDTokenFactory();

  public final String serverURI = "http://testServer.com";

  public final Map<String, User> users = new HashMap<String, User>();
  public final Map<String, String> sessionTokens = new HashMap<String, String>();
  public final Map<String, String> keyFetchTokens = new HashMap<String, String>();

  public static class User {
    public final String email;
    public final byte[] quickStretchedPW;
    public final String uid;
    public boolean verified;
    public final byte[] kA;
    public final byte[] wrapkB;

    public User(String email, byte[] quickStretchedPW) {
      this.email = email;
      this.quickStretchedPW = quickStretchedPW;
      this.uid = "uid/" + this.email;
      this.verified = false;
      this.kA = Utils.generateRandomBytes(8);
      this.wrapkB = Utils.generateRandomBytes(8);
    }
  }

  protected LoginResponse addLogin(User user, byte[] sessionToken, byte[] keyFetchToken) {
    // byte[] sessionToken = Utils.generateRandomBytes(8);
    if (sessionToken != null) {
      sessionTokens.put(Utils.byte2Hex(sessionToken), user.email);
    }
    // byte[] keyFetchToken = Utils.generateRandomBytes(8);
    if (keyFetchToken != null) {
      keyFetchTokens.put(Utils.byte2Hex(keyFetchToken), user.email);
    }
    return new LoginResponse(serverURI, user.uid, user.verified, sessionToken, keyFetchToken);
  }

  public void addUser(MockFxAccount fxAccount) throws UnsupportedEncodingException {
    String email = new String(fxAccount.emailUTF8, "UTF-8");
    User user = new User(email, fxAccount.quickStretchedPW);
    users.put(email, user);
    if (fxAccount.verified) {
      verifyUser(fxAccount);
    }
    addLogin(user, fxAccount.sessionToken, fxAccount.keyFetchToken);
  }

  public void verifyUser(MockFxAccount fxAccount) throws UnsupportedEncodingException {
    String email = new String(fxAccount.emailUTF8, "UTF-8");
    users.get(email).verified = true;
  }

  public void clearAllUserTokens() throws UnsupportedEncodingException {
    sessionTokens.clear();
    keyFetchTokens.clear();
  }

  protected BasicHttpResponse makeHttpResponse(int statusCode, String body) {
    BasicHttpResponse httpResponse = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), statusCode, body);
    try {
      httpResponse.setEntity(new StringEntity(body, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      // Whatever.
    }
    return httpResponse;
  }


  protected <T> void handleAuthFailure(RequestDelegate<T> requestDelegate, String message) {
    requestDelegate.handleFailure(new FxAccountClientRemoteException(makeHttpResponse(401, message),
        401, 1, "Bad authorization", message, null));
  }

  @Override
  public void status(byte[] sessionToken, RequestDelegate<StatusResponse> requestDelegate) {
    String email = sessionTokens.get(Utils.byte2Hex(sessionToken));
    User user = users.get(email);
    if (email == null || user == null) {
      handleAuthFailure(requestDelegate, "invalid sessionToken");
      return;
    }
    requestDelegate.handleSuccess(new StatusResponse(email, user.verified));
  }

  @Override
  public void loginAndGetKeys(byte[] emailUTF8, byte[] quickStretchedPW, RequestDelegate<LoginResponse> requestDelegate) {
    User user;
    try {
      user = users.get(new String(emailUTF8, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      user = null;
    }
    if (user == null) {
      handleAuthFailure(requestDelegate, "invalid emailUTF8");
      return;
    }
    if (user.quickStretchedPW == null || !Arrays.equals(user.quickStretchedPW, quickStretchedPW)) {
      handleAuthFailure(requestDelegate, "invalid quickStretchedPW");
      return;
    }
    LoginResponse loginResponse = addLogin(user, Utils.generateRandomBytes(8), Utils.generateRandomBytes(8));
    requestDelegate.handleSuccess(loginResponse);
  }

  @Override
  public void keys(byte[] keyFetchToken, RequestDelegate<TwoKeys> requestDelegate) {
    String email = keyFetchTokens.get(Utils.byte2Hex(keyFetchToken));
    User user = users.get(email);
    if (email == null || user == null) {
      handleAuthFailure(requestDelegate, "invalid keyFetchToken");
      return;
    }
    requestDelegate.handleSuccess(new TwoKeys(user.kA, user.wrapkB));
  }

  @Override
  public void sign(byte[] sessionToken, ExtendedJSONObject publicKey, long certificateDurationInMilliseconds, RequestDelegate<String> requestDelegate) {
    String email = sessionTokens.get(Utils.byte2Hex(sessionToken));
    User user = users.get(email);
    if (email == null || user == null) {
      handleAuthFailure(requestDelegate, "invalid sessionToken");
      return;
    }
    try {
      String certificate = mockMyIdTokenFactory.createMockMyIDCertificate(RSACryptoImplementation.createPublicKey(publicKey), "test", System.currentTimeMillis(), certificateDurationInMilliseconds);
      requestDelegate.handleSuccess(certificate);
    } catch (Exception e) {
      requestDelegate.handleError(e);
    }
  }
}
