package org.mozilla.gecko.sync.net;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.mozilla.apache.commons.codec.binary.Base64;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.tokenserver.TokenServerToken;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.client.methods.HttpUriRequest;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.message.BasicHeader;
import ch.boye.httpclientandroidlib.protocol.BasicHttpContext;

public class HMACAuthHeaderProvider implements AuthHeaderProvider {
  public static final String LOG_TAG = "HMACAuthHeaderProvider";

  public static final int NONCE_BYTES = 8;

  public static final String HMAC_SHA1_ALGORITHM = "hmacSHA1";

  public final String identifier;
  public final String key;

  public HMACAuthHeaderProvider(String identifier, String key) {
    this.identifier = identifier;
    this.key = key;
  }

  public HMACAuthHeaderProvider(TokenServerToken token) {
    this(token.id, token.key);
  }

  @Override
  public Header getAuthHeader(HttpRequestBase request, BasicHttpContext context, DefaultHttpClient client) {
    long timestamp = System.currentTimeMillis() / 1000;
    String nonce = Base64.encodeBase64String(Utils.generateRandomBytes(NONCE_BYTES));
    String extra = "";

    return getAuthHeader(request, context, client, timestamp, nonce, extra);
  }

  // For testing.
  public Header getAuthHeader(HttpRequestBase request, BasicHttpContext context, DefaultHttpClient client,
      long timestamp, String nonce, String extra) {
    String requestString = getRequestString(request, timestamp, nonce, extra);

    String macString;
    try {
      macString = getSignature(requestString, key);
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Got exception computing MAC authentication. " +
          "Returning null Authorization header.", e);
      return null;
    }

    String h = "MAC id=\"" + identifier + "\", " +
               "ts=\""     + timestamp  + "\", " +
               "nonce=\""  + nonce      + "\", " +
               "mac=\""    + macString  + "\"";

    if (extra != null) {
      h += ", ext=\"" + extra +"\"";
    }

    Header header = new BasicHeader("Authorization", h);

    return header;
  }

  protected static byte[] sha1(byte[] message, byte[] key)
      throws NoSuchAlgorithmException, InvalidKeyException {

    SecretKeySpec keySpec = new SecretKeySpec(key, HMAC_SHA1_ALGORITHM);

    Mac hasher = Mac.getInstance(HMAC_SHA1_ALGORITHM);
    hasher.init(keySpec);
    hasher.update(message);

    byte[] hmac = hasher.doFinal();

    return hmac;
  }

  public static String getSignature(String requestString, String key)
      throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
    String macString = Base64.encodeBase64String(sha1(requestString.getBytes("UTF-8"), key.getBytes("UTF-8")));

    return macString;
  }

  public static String getRequestString(HttpUriRequest request, long timestampInSeconds, String nonce, String extra) {
    URI uri = request.getURI();

    String method = request.getMethod().toUpperCase();

    String host = uri.getHost();

    String path = uri.getRawPath();
    if (uri.getRawQuery() != null) {
      path += "?";
      path += uri.getRawQuery();
    }
    if (uri.getRawFragment() != null) {
      path += "#";
      path += uri.getRawFragment();
    }

    int port = uri.getPort();
    String scheme = uri.getScheme();
    if (port != -1) {
    } else if ("http".equalsIgnoreCase(scheme)) {
      port = 80;
    } else if ("https".equalsIgnoreCase(scheme)) {
      port = 443;
    } else {
      throw new IllegalArgumentException("Unsupported URI scheme: " + scheme + ".");
    }

    String requestString = timestampInSeconds + "\n" +
        nonce       + "\n" +
        method      + "\n" +
        path        + "\n" +
        host        + "\n" +
        port        + "\n" +
        extra       + "\n";

    return requestString;
  }
}
