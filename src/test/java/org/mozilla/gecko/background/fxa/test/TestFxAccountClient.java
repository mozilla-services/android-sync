/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.fxa.test;

import java.security.KeyPair;
import java.util.concurrent.Executors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.background.fxa.FxAccount;
import org.mozilla.gecko.background.fxa.FxAccountClient;
import org.mozilla.gecko.background.fxa.FxAccountClient.TwoKeys;
import org.mozilla.gecko.background.fxa.FxAccountClient.TwoTokens;
import org.mozilla.gecko.background.fxa.PICLClientSideKeyStretcher;
import org.mozilla.gecko.background.fxa.crypto.DSAKeyGenerator;
import org.mozilla.gecko.background.fxa.crypto.KeyGenerator;
import org.mozilla.gecko.background.fxa.crypto.RSAKeyGenerator;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.HTTPFailureException;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.test.SpongyCastlePBKDF2;
import org.mozilla.gecko.sync.crypto.test.SpongyCastleScrypt;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

import ch.boye.httpclientandroidlib.HttpResponse;

public class TestFxAccountClient {
  private static final String TEST_SERVERURI = "http://127.0.0.1:9000";

  // The picl-idp is not yet a fan of unicode in email addresses.
  private static final String TEST_EMAIL = "andre@example.org";
  private static final String TEST_PASSWORD = "password";

  // If these don't stay fixed, we have to delete the account every test.
  private static final String TEST_MAINSALT = "" +
      "00f0000000000000" +
      "0000000000000000" +
      "0000000000000000" +
      "000000000000034d";
  private static final String TEST_SRPSALT = "" +
      "00f1000000000000" +
      "0000000000000000" +
      "0000000000000000" +
      "0000000000000179";

  public PICLClientSideKeyStretcher keyStretcher;
  public FxAccountClient client;

  @Before
  public void setUp() {
    BaseResource.rewriteLocalhost = false;
    keyStretcher = new PICLClientSideKeyStretcher(new SpongyCastlePBKDF2(), new SpongyCastleScrypt());
    client = new FxAccountClient(TEST_SERVERURI, Executors.newSingleThreadExecutor());
  }

  public abstract static class BaseDelegate<T> implements FxAccountClient.RequestDelegate<T> {
    protected final WaitHelper waitHelper;

    public BaseDelegate(WaitHelper waitHelper) {
      this.waitHelper = waitHelper;
    }

    @Override
    public void handleFailure(int status, HttpResponse response) {
      waitHelper.performNotify(new HTTPFailureException(new SyncStorageResponse(response)));
    }

    @Override
    public void handleError(Exception e) {
      waitHelper.performNotify(e);
    }
  }

  protected String createAccount(final FxAccount account) throws Throwable {
    final String[] uids = new String[1];
    final WaitHelper waitHelper = WaitHelper.getTestWaiter();
    try {
      waitHelper.performWait(new Runnable() {
        @Override
        public void run() {
          client.createAccount(account, new BaseDelegate<String>(waitHelper) {
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

  protected String certificateSign(final ExtendedJSONObject publicKey, final int duration, final byte[] sessionToken) {
    final String ret[] =  new String[1];
    final WaitHelper waitHelper = WaitHelper.getTestWaiter();
    waitHelper.performWait(new Runnable() {
      @Override
      public void run() {
        client.sign(sessionToken, publicKey, duration, new BaseDelegate<String>(waitHelper) {
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
  public void testCreateFlow() throws Throwable {
    doTestCreateAndLogin(TEST_EMAIL, TEST_PASSWORD, TEST_MAINSALT, TEST_SRPSALT);

    doTestCreateAndLogin("andré@example.org", "pässwörd",
        Utils.byte2hex(Utils.generateRandomBytes(32), 32),
        Utils.byte2hex(Utils.generateRandomBytes(32), 32));
  }

  protected void doTestCreateAndLogin(String email, String password, String mainSalt, String srpSalt) throws Throwable {
    byte[] stretchedPWBytes = keyStretcher.stretch(email, password, null);
    FxAccount account = FxAccount.makeFxAccount(email, stretchedPWBytes, mainSalt, srpSalt);

    String uid = null;
    try {
      uid = createAccount(account);
      Assert.assertNotNull(uid);
    } catch (Throwable e) {
      if (!(e instanceof HTTPFailureException)) {
        throw e;
      }
      SyncStorageResponse response = ((HTTPFailureException) e).response;
      Assert.assertEquals("Got status " + response.getStatusCode() + " with body: " + response.body(), 400, response.getStatusCode());
    }

    try {
      createAccount(account);
      Assert.fail();
    } catch (Throwable e) {
      if (!(e instanceof HTTPFailureException)) {
        throw e;
      }
      SyncStorageResponse response = ((HTTPFailureException) e).response;
      Assert.assertEquals("Got status " + response.getStatusCode() + " with body: " + response.body(), 400, response.getStatusCode());
    }

    byte[] authToken = login(email, stretchedPWBytes);

    TwoTokens tokens = sessionCreate(authToken);

    final byte[] keyFetchToken = tokens.keyFetchToken;
    TwoKeys keys = keys(keyFetchToken);
    System.out.println("kA    : " + Utils.byte2hex(keys.kA));
    System.out.println("wrapkB: " + Utils.byte2hex(keys.wrapkB));
  }

  @Test
  public void testSessionDestroy() throws Throwable {
    final byte[] stretchedPWBytes = keyStretcher.stretch(TEST_EMAIL, TEST_PASSWORD, null);
    FxAccount account = FxAccount.makeFxAccount(TEST_EMAIL, stretchedPWBytes, TEST_MAINSALT, TEST_SRPSALT);

    String uid = null;
    try {
      uid = createAccount(account);
      Assert.assertNotNull(uid);
    } catch (Throwable e) {
      if (!(e instanceof HTTPFailureException)) {
        throw e;
      }
      SyncStorageResponse response = ((HTTPFailureException) e).response;
      Assert.assertEquals("Got status " + response.getStatusCode() + " with body: " + response.body(), 400, response.getStatusCode());
    }

    byte[] authToken1 = login(TEST_EMAIL, stretchedPWBytes);
    byte[] authToken2 = login(TEST_EMAIL, stretchedPWBytes);
    Assert.assertFalse(Utils.byte2hex(authToken1).equals(Utils.byte2hex(authToken2)));

    TwoTokens tokens1 = sessionCreate(authToken1);
    TwoTokens tokens2 = sessionCreate(authToken2);

    byte[] sessionToken1 = tokens1.sessionToken;
    byte[] sessionToken2 = tokens2.sessionToken;

    sessionDestroy(sessionToken1);
    sessionDestroy(sessionToken2);

    try {
      sessionDestroy(sessionToken2);
      Assert.fail("Expected second session destroy with token sessionToken2 to fail.");
    } catch (Throwable e) {
      if (!(e instanceof HTTPFailureException)) {
        throw e;
      }
      SyncStorageResponse response = ((HTTPFailureException) e).response;
      Assert.assertEquals("Got status " + response.getStatusCode() + " with body: " + response.body(), 401, response.getStatusCode());
    }

    try {
      sessionDestroy(sessionToken1);
      Assert.fail("Expected second session destroy with token sessionToken1 to fail.");
    } catch (Throwable e) {
      if (!(e instanceof HTTPFailureException)) {
        throw e;
      }
      SyncStorageResponse response = ((HTTPFailureException) e).response;
      Assert.assertEquals("Got status " + response.getStatusCode() + " with body: " + response.body(), 401, response.getStatusCode());
    }
  }

  @Test
  public void testCertificateSign() throws Throwable {
    final byte[] stretchedPWBytes = keyStretcher.stretch(TEST_EMAIL, TEST_PASSWORD, null);
    FxAccount account = FxAccount.makeFxAccount(TEST_EMAIL, stretchedPWBytes, TEST_MAINSALT, TEST_SRPSALT);

    String uid = null;
    try {
      uid = createAccount(account);
      Assert.assertNotNull(uid);
    } catch (Throwable e) {
      if (!(e instanceof HTTPFailureException)) {
        throw e;
      }
      SyncStorageResponse response = ((HTTPFailureException) e).response;
      Assert.assertEquals("Got status " + response.getStatusCode() + " with body: " + response.body(), 400, response.getStatusCode());
    }

    byte[] authToken = login(TEST_EMAIL, stretchedPWBytes);
    TwoTokens tokens = sessionCreate(authToken);
    byte[] sessionToken = tokens.sessionToken;

    KeyGenerator rsaKeyGenerator = new RSAKeyGenerator();
    KeyPair rsaKeyPair = rsaKeyGenerator.generateKeypair(1024);

    String rsaCert = certificateSign(rsaKeyGenerator.serializePublicKey(rsaKeyPair.getPublic()), 24*60*60, sessionToken);
    Assert.assertNotNull(rsaCert);
    Assert.assertEquals(3, rsaCert.split("\\.").length);

    KeyGenerator dsaKeyGenerator = new DSAKeyGenerator();
    KeyPair dsaKeyPair = dsaKeyGenerator.generateKeypair(1024);

    String dsaCert = certificateSign(dsaKeyGenerator.serializePublicKey(dsaKeyPair.getPublic()), 24*60*60, sessionToken);
    Assert.assertNotNull(dsaCert);
    Assert.assertEquals(3, dsaCert.split("\\.").length);

    Assert.assertFalse(rsaCert.equals(dsaCert));
    System.out.println("rsaCert: " + rsaCert);
    System.out.println("dsaCert: " + dsaCert);
  }
}
