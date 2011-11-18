package org.mozilla.android.sync;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.mozilla.android.sync.crypto.CryptoException;
import org.mozilla.android.sync.crypto.CryptoInfo;
import org.mozilla.android.sync.crypto.Cryptographer;
import org.mozilla.android.sync.crypto.KeyBundle;
import org.mozilla.android.sync.crypto.MissingCryptoInputException;
import org.mozilla.android.sync.crypto.NoKeyBundleException;
import org.mozilla.android.sync.crypto.Utils;

public class BaseCryptoRecord implements CryptoRecord {

  // JSON related constants.
  private static final String KEY_CIPHERTEXT = "ciphertext";
  private static final String KEY_HMAC       = "hmac";
  private static final String KEY_IV         = "IV";
  private static final String KEY_PAYLOAD    = "payload";

  /**
   * Helper method for doing actual decryption.
   * 
   * Input: JSONObject containing a valid payload (cipherText, IV, HMAC),
   * KeyBundle with keys for decryption. Output: byte[] clearText
   * @throws CryptoException 
   */
  private static byte[] decryptPayload(ExtendedJSONObject payload, KeyBundle keybundle) throws CryptoException {
    byte[] ciphertext = Base64.decodeBase64((String) payload.get(KEY_CIPHERTEXT));
    byte[] iv         = Base64.decodeBase64((String) payload.get(KEY_IV));
    byte[] hmac       = Utils.hex2Byte((String) payload.get(KEY_HMAC));
    return Cryptographer.decrypt(new CryptoInfo(ciphertext, iv, hmac, keybundle));
  }

  // The encrypted JSON body object.
  private ExtendedJSONObject body;

  // The decrypted JSON body object. Fields are copied from `body`.
  public ExtendedJSONObject  cleartext;
  public KeyBundle   keyBundle;

  public BaseCryptoRecord(ExtendedJSONObject body) {
    if (body == null) {
      throw new IllegalArgumentException(
          "No body provided to BaseCryptoRecord constructor.");
    }
    this.body = body;
  }

  public BaseCryptoRecord(String jsonString) throws IOException, ParseException, NonObjectJSONException {
    this(ExtendedJSONObject.parseJSONObject(jsonString));
  }

  @Override
  public void setKeyBundle(KeyBundle bundle) {
    this.keyBundle = bundle;
  }

  private JSONObject parseUTF8AsJSONObject(byte[] in)
      throws UnsupportedEncodingException, ParseException, NonObjectJSONException {
    Object obj = new JSONParser().parse(new String(in, "UTF-8"));
    if (obj instanceof JSONObject) {
      return (JSONObject) obj;
    } else {
      throw new NonObjectJSONException(obj);
    }
  }

  @Override
  public void decrypt() throws CryptoException, IOException, ParseException,
                       NonObjectJSONException {
    if (this.keyBundle == null) {
      throw new NoKeyBundleException();
    }

    ExtendedJSONObject payload = body.getJSONObject(KEY_PAYLOAD);

    // Check that payload contains all pieces for crypto.
    if (!payload.containsKey(KEY_CIPHERTEXT) ||
        !payload.containsKey(KEY_IV) ||
        !payload.containsKey(KEY_HMAC)) {
      throw new MissingCryptoInputException();
    }

    // There's no difference between handling the crypto/keys object and
    // anything else; we just get this.keyBundle from a different source.
    byte[] cleartext = decryptPayload(payload, this.keyBundle);
    JSONObject parsedCleartext = this.parseUTF8AsJSONObject(cleartext);
    this.cleartext = (ExtendedJSONObject) this.body.clone();
    this.cleartext.put(KEY_PAYLOAD, parsedCleartext);
    this.body = null;
  }

  @Override
  public void encrypt() throws CryptoException, UnsupportedEncodingException {
    if (this.keyBundle == null) {
      throw new NoKeyBundleException();
    }
    String cleartext = this.cleartext.toJSONString();
    byte[] cleartextBytes = cleartext.getBytes("UTF-8");
    CryptoInfo info = new CryptoInfo(cleartextBytes, this.keyBundle);
    Cryptographer.encrypt(info);
    String message = new String(Base64.encodeBase64(info.getMessage()));
    String iv      = new String(Base64.encodeBase64(info.getIV()));
    String hmac    = Utils.byte2hex(info.getHMAC());
    this.body      = new ExtendedJSONObject();
    body.put(KEY_CIPHERTEXT, message);
    body.put(KEY_HMAC, hmac);
    body.put(KEY_IV, iv);
    this.cleartext = null;
  }
}
