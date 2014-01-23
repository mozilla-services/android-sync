/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.fxa.test;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.Executors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mozilla.android.sync.test.integration.IntegrationTestCategory;
import org.mozilla.apache.commons.codec.binary.Base64;
import org.mozilla.gecko.background.fxa.FxAccountClient10;
import org.mozilla.gecko.background.fxa.FxAccountClient10.TwoKeys;
import org.mozilla.gecko.background.fxa.FxAccountClient10.TwoTokens;
import org.mozilla.gecko.background.fxa.FxAccountClientException.FxAccountClientRemoteException;
import org.mozilla.gecko.background.testhelpers.WaitHelper;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.HTTPFailureException;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

@Category(IntegrationTestCategory.class)
public class TestLiveFxAccountClient10 {
  protected static final String TEST_SERVERURI = "http://127.0.0.1:9000/";
  // These tests fail against the live dev server because the accounts created
  // need to be manually verified.
  // protected static final String TEST_SERVERURI = "https://api-accounts.dev.lcip.org/";

  protected static final String TEST_EMAIL = "andre@example.org";
  protected static final String TEST_PASSWORD = "password";

  protected static final String TEST_EMAIL_UNICODE = "andré@example.org";
  protected static final String TEST_PASSWORD_UNICODE = "pässwörd";

  protected static final String TEST_EMAIL_TURKISH = "Atatürk@turkish.abcçdefgğhıijklmnoöprsştuüvyz.org";
  protected static final String TEST_PASSWORD_TURKISH = "İIiı";

  // If these don't stay fixed, we have to delete the accounts every test.
  protected static final String TEST_MAINSALT = "" +
      "00f0000000000000" +
      "0000000000000000" +
      "0000000000000000" +
      "000000000000034d";
  protected static final String TEST_SRPSALT = "" +
      "00f1000000000000" +
      "0000000000000000" +
      "0000000000000000" +
      "0000000000000179";

  public FxAccountClient10 client;

  @Before
  public void setUp() {
    BaseResource.rewriteLocalhost = false;
    client = new FxAccountClient10(TEST_SERVERURI, Executors.newSingleThreadExecutor());
  }

  public abstract static class BaseDelegate<T> implements FxAccountClient10.RequestDelegate<T> {
    protected final WaitHelper waitHelper;

    public BaseDelegate(WaitHelper waitHelper) {
      this.waitHelper = waitHelper;
    }

    @Override
    public void handleFailure(FxAccountClientRemoteException e) {
      waitHelper.performNotify(e);
    }

    @Override
    public void handleError(Exception e) {
      waitHelper.performNotify(e);
    }
  }

  protected String createAccount(final String email, final byte[] stretchedPWBytes, final String mainSalt, final String srpSalt)
      throws Throwable {
    final String[] uids = new String[1];
    final WaitHelper waitHelper = WaitHelper.getTestWaiter();
    try {
      waitHelper.performWait(new Runnable() {
        @Override
        public void run() {
          client.createAccount(email, stretchedPWBytes, mainSalt, srpSalt, new BaseDelegate<String>(waitHelper) {
            @Override
            public void handleSuccess(String uid) {
              uids[0] = uid;
              waitHelper.performNotify();
            }
          });
        }
      });
    } catch (WaitHelper.InnerError e) {
      throw e.innerError;
    }
    return uids[0];
  }

  protected TwoTokens sessionCreate(final byte[] authToken) {
    final TwoTokens[] twoTokens = new TwoTokens[1];
    final WaitHelper waitHelper = WaitHelper.getTestWaiter();
    waitHelper.performWait(new Runnable() {
      @Override
      public void run() {
        client.sessionCreate(authToken, new BaseDelegate<TwoTokens>(waitHelper) {
          @Override
          public void handleSuccess(TwoTokens tokens) {
            twoTokens[0] = tokens;
            waitHelper.performNotify();
          }
        });
      }
    });
    return twoTokens[0];
  }

  protected void sessionDestroy(final byte[] sessionToken) throws Throwable {
    final WaitHelper waitHelper = WaitHelper.getTestWaiter();
    try {
      waitHelper.performWait(new Runnable() {
        @Override
        public void run() {
          client.sessionDestroy(sessionToken, new BaseDelegate<Void>(waitHelper) {
            @Override
            public void handleSuccess(Void v) {
              waitHelper.performNotify();
            }
          });
        }
      });
    } catch (WaitHelper.InnerError e) {
      throw e.innerError;
    }
  }

  protected TwoKeys keys(final byte[] token) {
    final TwoKeys[] twoKeys = new TwoKeys[1];
    final WaitHelper waitHelper = WaitHelper.getTestWaiter();
    waitHelper.performWait(new Runnable() {
      @Override
      public void run() {
        client.keys(token, new BaseDelegate<TwoKeys>(waitHelper) {
          @Override
          public void handleSuccess(TwoKeys keys) {
            twoKeys[0] = keys;
            waitHelper.performNotify();
          }
        });
      }
    });
    return twoKeys[0];
  }

  protected byte[] login(final String email, final byte[] stretchedPWBytes) {
    final byte[] authTokens[] =  new byte[1][];
    final WaitHelper waitHelper = WaitHelper.getTestWaiter();
    waitHelper.performWait(new Runnable() {
      @Override
      public void run() {
        client.login(email, stretchedPWBytes, new BaseDelegate<byte[]>(waitHelper) {
          @Override
          public void handleSuccess(byte[] authToken) {
            authTokens[0] = authToken;
            waitHelper.performNotify();
          }
        });
      }
    });
    return authTokens[0];
  }

  protected String certificateSign(final ExtendedJSONObject publicKey, final int durationInMilliseconds, final byte[] sessionToken) {
    final String ret[] =  new String[1];
    final WaitHelper waitHelper = WaitHelper.getTestWaiter();
    waitHelper.performWait(new Runnable() {
      @Override
      public void run() {
        client.sign(sessionToken, publicKey, durationInMilliseconds, new BaseDelegate<String>(waitHelper) {
          @Override
          public void handleSuccess(String cert) {
            ret[0] = cert;
            waitHelper.performNotify();
          }
        });
      }
    });
    return ret[0];
  }

  @Test
  public void testCreateDeterministic() throws Throwable {
    // Fixed account, no unicode.
    doTestCreateAndLogin(TEST_EMAIL, TEST_PASSWORD,
        TEST_MAINSALT, TEST_SRPSALT, true);

    // Fixed account, unicode.
    doTestCreateAndLogin(TEST_EMAIL_UNICODE, TEST_PASSWORD_UNICODE,
        TEST_MAINSALT, TEST_SRPSALT, true);

    // Fixed account, Turkish.
    doTestCreateAndLogin(TEST_EMAIL_TURKISH, TEST_PASSWORD_TURKISH,
        TEST_MAINSALT, TEST_SRPSALT, true);
  }

  @Test
  public void testCreateNonDeterministic() throws Throwable {
    // Random account.
    long now = System.currentTimeMillis();
    doTestCreateAndLogin("" + now + "@example.org", "" + now + "password",
        Utils.byte2Hex(Utils.generateRandomBytes(32), 64),
        Utils.byte2Hex(Utils.generateRandomBytes(32), 64),
        false);
  }

  protected byte[] stretchEmailAndPassword(String email, String password) throws UnsupportedEncodingException {
    return Utils.concatAll(email.getBytes("UTF-8"), password.getBytes("UTF-8"));
  }

  protected void doTestCreateAndLogin(final String email, String password, final String mainSalt, final String srpSalt, final boolean accountMayExist)
      throws Throwable {
    byte[] stretchedPWBytes = stretchEmailAndPassword(email, password);

    try {
      String uid = createAccount(email, stretchedPWBytes, mainSalt, srpSalt);
      Assert.assertNotNull(uid);
    } catch (HTTPFailureException e) {
      SyncStorageResponse response = e.response;
      if (accountMayExist) {
        Assert.assertEquals("Got status " + response.getStatusCode() + " with body: " + response.body(), 400, response.getStatusCode());
      } else {
        Assert.fail("Expected to create account but got status " + response.getStatusCode() + " with body: " + response.body());
      }
    }

    try {
      createAccount(email, stretchedPWBytes, mainSalt, srpSalt);
      Assert.fail();
    } catch (HTTPFailureException e) {
      SyncStorageResponse response = e.response;
      Assert.assertEquals("Got status " + response.getStatusCode() + " with body: " + response.body(), 400, response.getStatusCode());
    }

    byte[] authToken = login(email, stretchedPWBytes);

    TwoTokens tokens = sessionCreate(authToken);

    final byte[] keyFetchToken = tokens.keyFetchToken;
    TwoKeys keys = keys(keyFetchToken);

    Assert.assertEquals(32, keys.kA.length);
    Assert.assertEquals(32, keys.wrapkB.length);
  }

  @Test
  public void testSessionDestroy() throws Throwable {
    byte[] stretchedPWBytes = stretchEmailAndPassword(TEST_EMAIL, TEST_PASSWORD);

    String uid = null;
    try {
      uid = createAccount(TEST_EMAIL, stretchedPWBytes, TEST_MAINSALT, TEST_SRPSALT);
      Assert.assertNotNull(uid);
    } catch (HTTPFailureException e) {
      SyncStorageResponse response = e.response;
      Assert.assertEquals("Got status " + response.getStatusCode() + " with body: " + response.body(), 400, response.getStatusCode());
    }

    byte[] authToken1 = login(TEST_EMAIL, stretchedPWBytes);
    byte[] authToken2 = login(TEST_EMAIL, stretchedPWBytes);
    Assert.assertFalse(Utils.byte2Hex(authToken1).equals(Utils.byte2Hex(authToken2)));

    TwoTokens tokens1 = sessionCreate(authToken1);
    TwoTokens tokens2 = sessionCreate(authToken2);

    byte[] sessionToken1 = tokens1.sessionToken;
    byte[] sessionToken2 = tokens2.sessionToken;

    sessionDestroy(sessionToken1);
    sessionDestroy(sessionToken2);

    try {
      sessionDestroy(sessionToken2);
      Assert.fail("Expected second session destroy with token sessionToken2 to fail.");
    } catch (HTTPFailureException e) {
      SyncStorageResponse response = e.response;
      Assert.assertEquals("Got status " + response.getStatusCode() + " with body: " + response.body(), 401, response.getStatusCode());
    }

    try {
      sessionDestroy(sessionToken1);
      Assert.fail("Expected second session destroy with token sessionToken1 to fail.");
    } catch (HTTPFailureException e) {
      SyncStorageResponse response = e.response;
      Assert.assertEquals("Got status " + response.getStatusCode() + " with body: " + response.body(), 401, response.getStatusCode());
    }
  }

  @Test
  public void testCertificateSign() throws Throwable {
    byte[] stretchedPWBytes = stretchEmailAndPassword(TEST_EMAIL, TEST_PASSWORD);

    String uid = null;
    try {
      uid = createAccount(TEST_EMAIL, stretchedPWBytes, TEST_MAINSALT, TEST_SRPSALT);
      Assert.assertNotNull(uid);
    } catch (HTTPFailureException e) {
      SyncStorageResponse response = e.response;
      Assert.assertEquals("Got status " + response.getStatusCode() + " with body: " + response.body(), 400, response.getStatusCode());
    }

    byte[] authToken = login(TEST_EMAIL, stretchedPWBytes);
    TwoTokens tokens = sessionCreate(authToken);
    byte[] sessionToken = tokens.sessionToken;

    // A randomly generated 1024-bit RSA public key.
    ExtendedJSONObject rsaPublicKey = new ExtendedJSONObject("{\"e\":\"65537\",\"n\":\"115428212032523452360559156665721747608377117676278290767719725156252183292299998074735133019809510582031617251668008573711272930175845035980898799669418910773198981599635706542417546226756387073530955166563539348743667117528439082949061747701231556252937480982686996994550810751179296614212752420100673005823\",\"algorithm\":\"RS\"}");
    String rsaCert = certificateSign(rsaPublicKey, 24*60*60*1000, sessionToken);
    Assert.assertNotNull(rsaCert);
    Assert.assertEquals(3, rsaCert.split("\\.").length);

    // A randomly generated 1024-bit DSA public key.
    ExtendedJSONObject dsaPublicKey = new ExtendedJSONObject("{\"g\":\"f7e1a085d69b3ddecbbcab5c36b857b97994afbbfa3aea82f9574c0b3d0782675159578ebad4594fe67107108180b449167123e84c281613b7cf09328cc8a6e13c167a8b547c8d28e0a3ae1e2bb3a675916ea37f0bfa213562f1fb627a01243bcca4f1bea8519089a883dfe15ae59f06928b665e807b552564014c3bfecf492a\",\"q\":\"9760508f15230bccb292b982a2eb840bf0581cf5\",\"p\":\"fd7f53811d75122952df4a9c2eece4e7f611b7523cef4400c31e3f80b6512669455d402251fb593d8d58fabfc5f5ba30f6cb9b556cd7813b801d346ff26660b76b9950a5a49f9fe8047b1022c24fbba9d7feb7c61bf83b57e7c6a8a6150f04fb83f6d3c51ec3023554135a169132f675f3ae2b61d72aeff22203199dd14801c7\",\"y\":\"f46aec879652771391df0317cffbbd38d10ddcfbe65b95ec1e1e62509a27e3edb9aecea876db478a7e89d79622da564c409a63a95a2e567fa59187ecf3fc7ddd87ed4ec3f77a75ad27b6351e2c3f8967d400ba72271fcbec751ea8a50c72871efb5a64ea0cbfb33d9d77fbf3cecd1e51c757ca58553c5f38b47f9c8d8deb8b85\",\"algorithm\":\"DS\"}");
    String dsaCert = certificateSign(dsaPublicKey, 24*60*60*1000, sessionToken);
    Assert.assertNotNull(dsaCert);
    Assert.assertEquals(3, dsaCert.split("\\.").length);

    Assert.assertFalse(rsaCert.equals(dsaCert));

    ExtendedJSONObject cert = new ExtendedJSONObject(new String(Base64.decodeBase64(rsaCert.split("\\.")[1]), "UTF-8"));
    Assert.assertNotNull(cert.getObject("principal"));
    String email = cert.getObject("principal").getString("email");
    Assert.assertEquals(TEST_SERVERURI.split("//")[1].split("/")[0], email.split("@")[1]);
  }
}
