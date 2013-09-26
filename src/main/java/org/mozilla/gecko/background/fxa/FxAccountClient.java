/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.fxa;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.Executor;

import javax.crypto.Mac;

import org.json.simple.JSONObject;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.HKDF;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;
import org.mozilla.gecko.sync.net.HawkAuthHeaderProvider;
import org.mozilla.gecko.sync.net.Resource;
import org.mozilla.gecko.sync.net.SyncResponse;

import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;

/**
 * An HTTP client for talking to an FxAccount server.
 * <p>
 * The reference server is developed at
 * <a href="https://github.com/mozilla/picl-idp">https://github.com/mozilla/picl-idp</a>.
 * This implementation was developed against
 * <a href="https://github.com/mozilla/picl-idp/commit/c7a02a0cbbb43f332058dc060bd84a21e56ec208">https://github.com/mozilla/picl-idp/commit/c7a02a0cbbb43f332058dc060bd84a21e56ec208</a>.
 */
public class FxAccountClient {
  protected static final String LOG_TAG = FxAccountClient.class.getSimpleName();

  protected final String serverURI;
  protected final Executor executor;

  public FxAccountClient(String serverURI, Executor executor) {
    if (serverURI == null) {
      throw new IllegalArgumentException("Must provide a server URI.");
    }
    if (executor == null) {
      throw new IllegalArgumentException("Must provide a non-null executor.");
    }
    this.serverURI = serverURI.endsWith("/") ? serverURI : serverURI + "/";
    this.executor = executor;
  }

  public interface RequestDelegate<T> {
    public void handleError(Exception e);
    public void handleFailure(int status, HttpResponse response);
    public void handleSuccess(T result);
  }

  /**
   * Thin container for two access tokens.
   */
  public static class TwoTokens {
    public final byte[] keyFetchToken;
    public final byte[] sessionToken;
    public TwoTokens(byte[] keyFetchToken, byte[] sessionToken) {
      this.keyFetchToken = keyFetchToken;
      this.sessionToken = sessionToken;
    }
  }

  /**
   * Thin container for two cryptographic keys.
   */
  public static class TwoKeys {
    public final byte[] kA;
    public final byte[] wrapkB;
    public TwoKeys(byte[] kA, byte[] wrapkB) {
      this.kA = kA;
      this.wrapkB = wrapkB;
    }
  }

  /**
   * Translate resource callbacks into request callbacks invoked on the provided
   * executor.
   * <p>
   * Override <code>handleSuccess</code> to parse the body of the resource
   * request and call the request callback. <code>handleSuccess</code> is
   * invoked via the executor, so you don't need to delegate further.
   */
  protected abstract class ResourceDelegate<T> extends BaseResourceDelegate {
    protected abstract void handleSuccess(final int status, HttpResponse response, final ExtendedJSONObject body);

    protected final RequestDelegate<T> delegate;

    protected final byte[] tokenId;
    protected final byte[] reqHMACKey;
    protected final boolean payload;

    /**
     * Create a delegate for an un-authenticated resource.
     */
    public ResourceDelegate(final Resource resource, final RequestDelegate<T> delegate) {
      this(resource, delegate, null, null, false);
    }

    /**
     * Create a delegate for a Hawk-authenticated resource.
     */
    public ResourceDelegate(final Resource resource, final RequestDelegate<T> delegate, final byte[] tokenId, final byte[] reqHMACKey, final boolean authenticatePayload) {
      super(resource);
      this.delegate = delegate;
      this.reqHMACKey = reqHMACKey;
      this.tokenId = tokenId;
      this.payload = authenticatePayload;
    }

    @Override
    public AuthHeaderProvider getAuthHeaderProvider() {
      if (tokenId != null && reqHMACKey != null) {
        return new HawkAuthHeaderProvider(Utils.byte2hex(tokenId), reqHMACKey, payload);
      }
      return super.getAuthHeaderProvider();
    }

    @Override
    public void handleHttpResponse(HttpResponse response) {
      final int status = response.getStatusLine().getStatusCode();
      switch (status) {
      case 200:
        invokeHandleSuccess(status, response);
        return;
      default:
        invokeHandleFailure(status, response);
        return;
      }
    }

    protected void invokeHandleError(final Exception e) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          delegate.handleError(e);
        }
      });
    }

    protected void invokeHandleFailure(final int status, final HttpResponse response) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          delegate.handleFailure(status, response);
        }
      });
    }

    protected void invokeHandleSuccess(final int status, final HttpResponse response) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          try {
            ExtendedJSONObject body = new SyncResponse(response).jsonObjectBody();
            ResourceDelegate.this.handleSuccess(status, response, body);
          } catch (Exception e) {
            delegate.handleError(e);
          }
        }
      });
    }

    @Override
    public void handleHttpProtocolException(final ClientProtocolException e) {
      invokeHandleError(e);
    }

    @Override
    public void handleHttpIOException(IOException e) {
      invokeHandleError(e);
    }

    @Override
    public void handleTransportException(GeneralSecurityException e) {
      invokeHandleError(e);
    }
  }

  protected <T> URI makeURI(String path, final RequestDelegate<T> delegate) {
    try {
      return new URI(serverURI + path);
    } catch (URISyntaxException e) {
      final Exception ex = e;
      executor.execute(new Runnable() {
        @Override
        public void run() {
          delegate.handleError(ex);
        }
      });
      return null;
    }
  }

  protected <T> void post(final BaseResource resource, final JSONObject requestBody, final RequestDelegate<T> delegate) {
    try {
      if (requestBody == null) {
        resource.post((HttpEntity) null);
      } else {
        resource.post(requestBody);
      }
    } catch (UnsupportedEncodingException e) {
      final Exception ex = e;
      executor.execute(new Runnable() {
        @Override
        public void run() {
          delegate.handleError(ex);
        }
      });
      return;
    }
  }

  @SuppressWarnings("unchecked")
  public void createAccount(FxAccount account, final RequestDelegate<String> delegate) {
    final JSONObject body = new JSONObject();
    body.put("email", FxAccountUtils.bytes(account.email));

    final JSONObject stretching = new JSONObject();
    stretching.put("type", "PBKDF2/scrypt/PBKDF2/v1");
    stretching.put("PBKDF2_rounds_1", 20000);
    stretching.put("scrypt_N", 65536);
    stretching.put("scrypt_r", 8);
    stretching.put("scrypt_p", 1);
    stretching.put("PBKDF2_rounds_2", 20000);
    stretching.put("salt", account.mainSalt);
    body.put("passwordStretching", stretching);

    final JSONObject srp = new JSONObject();
    srp.put("type", "SRP-6a/SHA256/2048/v1");
    srp.put("verifier", account.params.hexModN(account.v)); // XXX variable N.
    srp.put("salt", account.srpSalt);
    body.put("srp", srp);

    final BaseResource resource = new BaseResource(makeURI("account/create", delegate));
    resource.delegate = new ResourceDelegate<String>(resource, delegate) {
      @Override
      public void handleSuccess(int status, HttpResponse response, ExtendedJSONObject body) {
        String uid = body.getString("uid");
        if (uid == null) {
          delegate.handleError(new RuntimeException("uid must be a non-null string"));
          return;
        }
        delegate.handleSuccess(uid);
      }
    };
    post(resource, body, delegate);
  }

  @SuppressWarnings("unchecked")
  protected void authStart(final String email, final byte[] stretchedPWBytes, final RequestDelegate<SRPSession> delegate) {
    final JSONObject body = new JSONObject();
    body.put("email", FxAccountUtils.bytes(email));

    final BaseResource resource = new BaseResource(makeURI("auth/start", delegate));
    resource.delegate = new ResourceDelegate<SRPSession>(resource, delegate) {
      @Override
      public void handleSuccess(int status, HttpResponse response, ExtendedJSONObject body) {
        String srpToken = null;
        String srpSalt = null;
        String srpB = null;
        String mainSalt;
        try {
          srpToken = body.getString("srpToken");
          if (srpToken == null) {
            throw new RuntimeException("srpToken must be a non-null object");
          }
          ExtendedJSONObject srp = body.getObject("srp");
          if (srp == null) {
            throw new RuntimeException("srp must be a non-null object");
          }
          // XXX better error checking.
          srpSalt = srp.getString("salt");
          srpB = srp.getString("B");
          if (srpSalt == null) {
            throw new RuntimeException("srp.salt must not be null");
          }
          if (srpB == null) {
            throw new RuntimeException("srp.B must not be null");
          }

          //          "passwordStretching": {
          //            "type": "PBKDF2/scrypt/PBKDF2/v1",
          //            "PBKDF2_rounds_1": 20000,
          //            "scrypt_N": 65536,
          //            "scrypt_r": 8,
          //            "scrypt_p": 1,
          //            "PBKDF2_rounds_2": 20000,
          //            "salt": "996bc6b1aa63cd69856a2ec81cbf19d5c8a604713362df9ee15c2bf07128efab"
          //          }
          ExtendedJSONObject passwordStretching = body.getObject("passwordStretching");
          if (passwordStretching == null) {
            throw new RuntimeException("passwordStretching must be a non-null object");
          }
          mainSalt = passwordStretching.getString("salt");
          if (mainSalt == null) {
            throw new RuntimeException("srp.passwordStretching.salt must not be null");
          }

          FxAccount account = FxAccount.makeFxAccount(email, stretchedPWBytes, mainSalt, srpSalt);
          SRPSession srpSession = account.srpSession(srpB);
          srpSession.srpToken = srpToken;

          delegate.handleSuccess(srpSession);
        } catch (Exception e) {
          delegate.handleError(e);
          return;
        }
      }
    };
    post(resource, body, delegate);
  }

  public static String hexModN(BigInteger value, BigInteger N) {
    return Utils.byte2hex(Utils.hex2Byte(value.toString(16)), N.toString(16).length());
  }

  @SuppressWarnings("unchecked")
  protected void authFinish(final SRPSession srpSession, RequestDelegate<byte[]> delegate) {
    final JSONObject body = new JSONObject();
    body.put("srpToken", srpSession.srpToken);
    body.put("A", hexModN(srpSession.A, srpSession.N));
    body.put("M", Utils.byte2hex(srpSession.M));

    final BaseResource resource = new BaseResource(makeURI("auth/finish", delegate));
    resource.delegate = new ResourceDelegate<byte[]>(resource, delegate) {
      @Override
      public void handleSuccess(int status, HttpResponse response, ExtendedJSONObject body) {
        try {
          byte[] authToken = new byte[32];
          unbundleBody(body, srpSession.K, FxAccountUtils.KW("auth/finish"), authToken);
          delegate.handleSuccess(authToken);
        } catch (Exception e) {
          delegate.handleError(e);
          return;
        }
      }
    };
    post(resource, body, delegate);
  }

  public void login(final String email, final byte[] stretchedPWBytes, final RequestDelegate<byte[]> delegate) {
    authStart(email, stretchedPWBytes, new RequestDelegate<SRPSession>() {
      @Override
      public void handleSuccess(SRPSession srpSession) {
        authFinish(srpSession, delegate);
      }

      @Override
      public void handleError(final Exception e) {
        executor.execute(new Runnable() {
          @Override
          public void run() {
            delegate.handleError(e);
          }
        });
      }

      @Override
      public void handleFailure(final int status, final HttpResponse response) {
        executor.execute(new Runnable() {
          @Override
          public void run() {
            delegate.handleFailure(status, response);
          }
        });
      }
    });
  }

  public void sessionCreate(byte[] authToken, final RequestDelegate<TwoTokens> delegate) {
    final byte[] tokenId = new byte[32];
    final byte[] reqHMACKey = new byte[32];
    final byte[] requestKey = new byte[32];
    try {
      byte[] derived = HKDF.derive(authToken, new byte[0], FxAccountUtils.KW("authToken"), 3*32);
      System.arraycopy(derived, 0*32, tokenId, 0, 1*32);
      System.arraycopy(derived, 1*32, reqHMACKey, 0, 1*32);
      System.arraycopy(derived, 2*32, requestKey, 0, 1*32);
    } catch (Exception e) {
      final Exception ex = e;
      executor.execute(new Runnable() {
        @Override
        public void run() {
          delegate.handleError(ex);
        }
      });
      return;
    }

    final BaseResource resource = new BaseResource(makeURI("session/create", delegate));
    resource.delegate = new ResourceDelegate<TwoTokens>(resource, delegate, tokenId, reqHMACKey, false) {
      @Override
      public void handleSuccess(int status, HttpResponse response, ExtendedJSONObject body) {
        try {
          byte[] keyFetchToken = new byte[32];
          byte[] sessionToken = new byte[32];
          unbundleBody(body, requestKey, FxAccountUtils.KW("session/create"), keyFetchToken, sessionToken);
          delegate.handleSuccess(new TwoTokens(keyFetchToken, sessionToken));
          return;
        } catch (Exception e) {
          delegate.handleError(e);
          return;
        }
      }
    };
    post(resource, null, delegate);
  }

  public void sessionDestroy(byte[] sessionToken, final RequestDelegate<Void> delegate) {
    final byte[] tokenId = new byte[32];
    final byte[] reqHMACKey = new byte[32];
    try {
      byte[] derived = HKDF.derive(sessionToken, new byte[0], FxAccountUtils.KW("session"), 2*32);
      System.arraycopy(derived, 0*32, tokenId, 0, 1*32);
      System.arraycopy(derived, 1*32, reqHMACKey, 0, 1*32);
    } catch (Exception e) {
      final Exception ex = e;
      executor.execute(new Runnable() {
        @Override
        public void run() {
          delegate.handleError(ex);
        }
      });
      return;
    }

    final BaseResource resource = new BaseResource(makeURI("session/destroy", delegate));
    resource.delegate = new ResourceDelegate<Void>(resource, delegate, tokenId, reqHMACKey, false) {
      @Override
      public void handleSuccess(int status, HttpResponse response, ExtendedJSONObject body) {
        delegate.handleSuccess(null);
      }
    };
    post(resource, null, delegate);
  }

  protected void unbundleBytes(byte[] bundleBytes, byte[] respHMACKey, byte[] respXORKey, byte[]... rest)
      throws InvalidKeyException, NoSuchAlgorithmException {
    if (bundleBytes.length < 32) {
      throw new IllegalArgumentException("input bundle must include HMAC");
    }
    int len = respXORKey.length;
    if (bundleBytes.length != len + 32) {
      throw new IllegalArgumentException("input bundle and XOR key with HMAC have different lengths");
    }
    int left = len;
    for (byte[] array : rest) {
      left -= array.length;
    }
    if (left != 0) {
      throw new IllegalArgumentException("XOR key and total output arrays have different lengths");
    }

    byte[] ciphertext = new byte[len];
    byte[] HMAC = new byte[32];
    System.arraycopy(bundleBytes, 0, ciphertext, 0, len);
    System.arraycopy(bundleBytes, len, HMAC, 0, 32);

    Mac hmacHasher = HKDF.makeHMACHasher(respHMACKey);
    byte[] computedHMAC = hmacHasher.doFinal(ciphertext);
    if (!Arrays.equals(computedHMAC, HMAC)) {
      throw new RuntimeException("Bad message HMAC");
    }

    int offset = 0;
    for (byte[] array : rest) {
      for (int i = 0; i < array.length; i++) {
        array[i] = (byte) (respXORKey[offset + i] ^ ciphertext[offset + i]);
      }
      offset += array.length;
    }
  }

  protected void unbundleBody(ExtendedJSONObject body, byte[] requestKey, byte[] ctxInfo, byte[]... rest) throws Exception {
    int length = 0;
    for (byte[] array : rest) {
      length += array.length;
    }

    String bundle = body.getString("bundle");
    if (bundle == null) {
      throw new RuntimeException("bundle must be a non-null string");
    }
    byte[] bundleBytes = Utils.hex2Byte(bundle);

    final byte[] respHMACKey = new byte[32];
    final byte[] respXORKey = new byte[length];
    byte[] respKeys = HKDF.derive(requestKey, new byte[0], ctxInfo, length + 32);
    System.arraycopy(respKeys, 0*32, respHMACKey, 0, 1*32);
    System.arraycopy(respKeys, 1*32, respXORKey, 0, length);
    unbundleBytes(bundleBytes, respHMACKey, respXORKey, rest);
  }

  public void keys(byte[] keyFetchToken, final RequestDelegate<TwoKeys> delegate) {
    final byte[] tokenId = new byte[32];
    final byte[] reqHMACKey = new byte[32];
    final byte[] respHMACKey = new byte[32];
    final byte[] respXORKey = new byte[64];
    try {
      byte[] derived = HKDF.derive(keyFetchToken, new byte[0], FxAccountUtils.KW("account/keys"), 5*32);
      System.arraycopy(derived, 0*32, tokenId, 0, 1*32);
      System.arraycopy(derived, 1*32, reqHMACKey, 0, 1*32);
      System.arraycopy(derived, 2*32, respHMACKey, 0, 1*32);
      System.arraycopy(derived, 3*32, respXORKey, 0, 2*32);
    } catch (Exception e) {
      final Exception ex = e;
      executor.execute(new Runnable() {
        @Override
        public void run() {
          delegate.handleError(ex);
        }
      });
    }

    final BaseResource resource = new BaseResource(makeURI("account/keys", delegate));
    resource.delegate = new ResourceDelegate<TwoKeys>(resource, delegate, tokenId, reqHMACKey, false) {
      @Override
      public void handleSuccess(int status, HttpResponse response, ExtendedJSONObject body) {
        try {
          String bundle = body.getString("bundle");
          if (bundle == null) {
            delegate.handleError(new RuntimeException("bundle must be a non-null string"));
            return;
          }
          byte[] bundleBytes = Utils.hex2Byte(bundle);
          byte[] kA = new byte[32];
          byte[] wrapkB = new byte[32];
          unbundleBytes(bundleBytes, respHMACKey, respXORKey, kA, wrapkB);
          delegate.handleSuccess(new TwoKeys(kA, wrapkB));
          return;
        } catch (Exception e) {
          delegate.handleError(e);
          return;
        }
      }
    };
    resource.get();
  }

  @SuppressWarnings("unchecked")
  public void sign(final byte[] sessionToken, final ExtendedJSONObject publicKey, int durationInSeconds, final RequestDelegate<String> delegate) {
    final JSONObject body = new JSONObject();
    body.put("publicKey", publicKey);
    body.put("duration", durationInSeconds);

    final byte[] tokenId = new byte[32];
    final byte[] reqHMACKey = new byte[32];
    try {
      byte[] derived = HKDF.derive(sessionToken, new byte[0], FxAccountUtils.KW("session"), 2*32);
      System.arraycopy(derived, 0*32, tokenId, 0, 1*32);
      System.arraycopy(derived, 1*32, reqHMACKey, 0, 1*32);
    } catch (Exception e) {
      final Exception ex = e;
      executor.execute(new Runnable() {
        @Override
        public void run() {
          delegate.handleError(ex);
        }
      });
    }

    final BaseResource resource = new BaseResource(makeURI("certificate/sign", delegate));
    resource.delegate = new ResourceDelegate<String>(resource, delegate, tokenId, reqHMACKey, true) {
      @Override
      public void handleSuccess(int status, HttpResponse response, ExtendedJSONObject body) {
        String cert = body.getString("cert");
        if (cert == null) {
          delegate.handleError(new RuntimeException("cert must be a non-null string"));
          return;
        }
        delegate.handleSuccess(cert);
      }
    };
    post(resource, body, delegate);
  }
}
