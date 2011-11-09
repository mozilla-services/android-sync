package org.mozilla.android.sync;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.mozilla.android.sync.domain.CryptoInfo;
import org.mozilla.android.sync.domain.KeyBundle;

public class BaseCryptoRecord implements CryptoRecord {

  // JSON related constants.
  private static final String KEY_CIPHERTEXT        = "ciphertext";
  private static final String KEY_HMAC               = "hmac";
  private static final String KEY_IV                 = "IV";
  private static final String KEY_PAYLOAD            = "payload";
  private static final String KEY_ID                 = "id";
  private static final String KEY_COLLECTION         = "collection";
  private static final String KEY_COLLECTIONS        = "collections";
  private static final String KEY_DEFAULT_COLLECTION = "default";

  private static final String ID_CRYPTO_KEYS         = "keys";
  private static final String CRYPTO_KEYS_COLLECTION = "crypto";

  /**
   * Helper method to get a JSONObject from a String. Input: String containing
   * JSON. Output: Extracted JSONObject. Throws: Exception if JSON is invalid.
   * 
   * @throws ParseException
   * @throws IOException
   * @throws NonObjectJSONException
   *           If the object is valid JSON, but not an object.
   */
  private static JSONObject getJSONObject(String jsonString)
                                                            throws IOException,
                                                            ParseException,
                                                            NonObjectJSONException {
    Reader in = new StringReader(jsonString);
    Object obj = new JSONParser().parse(in);
    if (obj instanceof JSONObject) {
      return (JSONObject) obj;
    } else {
      throw new NonObjectJSONException(obj);
    }
  }

  /**
   * Helper method for extracting a JSONObject from its string encoding within
   * another JSONObject.
   * 
   * Input: JSONObject and key. Output: JSONObject extracted. Throws: Exception
   * if JSON is invalid.
   * 
   * @throws NonObjectJSONException
   * @throws ParseException
   * @throws IOException
   */
  private static JSONObject getJSONObject(JSONObject json, String key)
                                                                      throws IOException,
                                                                      ParseException,
                                                                      NonObjectJSONException {
    return getJSONObject((String) json.get(key));
  }

  /**
   * Helper method for doing actual decryption.
   * 
   * Input: JSONObject containing a valid payload (cipherText, IV, HMAC),
   * KeyBundle with keys for decryption. Output: byte[] clearText
   * @throws CryptoException 
   */
  private static byte[] decryptPayload(JSONObject payload, KeyBundle keybundle) throws CryptoException {
    System.out.println("Ciphertext is " + payload.get(KEY_CIPHERTEXT));
    byte[] ciphertext = Base64.decodeBase64((String) payload.get(KEY_CIPHERTEXT));
    byte[] iv = Base64.decodeBase64((String) payload.get(KEY_IV));
    byte[] hmac = Utils.hex2Byte((String) payload.get(KEY_HMAC));
    CryptoInfo info = new CryptoInfo(ciphertext, iv, hmac, keybundle);
    return Cryptographer.decrypt(info);
  }

  // The encrypted JSON body object.
  private JSONObject body;

  // The decrypted JSON body object. Fields are copied from `body`.
  public JSONObject  cleartext;
  public KeyBundle   keyBundle;

  public BaseCryptoRecord(JSONObject body) {
    if (body == null) {
      throw new IllegalArgumentException(
          "No body provided to BaseCryptoRecord constructor.");
    }
    this.body = body;
  }

  @Override
  public void setKeyBundle(KeyBundle bundle) {
    this.keyBundle = bundle;
  }

  @Override
  public void decrypt() throws CryptoException, IOException, ParseException,
                       NonObjectJSONException {
    if (this.keyBundle == null) {
      throw new NoKeyBundleException();
    }
    System.out.println("Attempting to decrypt record.");
    System.out.println("JSON object: " + this.body.toJSONString());

    JSONObject payload = getJSONObject(body, KEY_PAYLOAD);

    // Check that payload contains all pieces for crypto.
    if (!payload.containsKey(KEY_CIPHERTEXT) ||
        !payload.containsKey(KEY_IV) ||
        !payload.containsKey(KEY_HMAC)) {
      throw new MissingCryptoInputException();
    }

    // There's no difference between handling the crypto/keys object and
    // anything else; we just get this.keyBundle from a different source.
    byte[] cleartext = decryptPayload(payload, this.keyBundle);
    Object parsedCleartext = new JSONParser().parse(new String(cleartext,
        "UTF-8"));
    if (parsedCleartext instanceof JSONObject) {
      this.cleartext = (JSONObject) this.body.clone();
      Utils.asMap(this.cleartext).put(KEY_PAYLOAD, parsedCleartext);
    } else {
      throw new NonObjectJSONException(parsedCleartext);
    }
  }

  @Override
  public void encrypt() throws CryptoException {
    if (this.keyBundle == null) {
      throw new NoKeyBundleException();
    }
  }
}
