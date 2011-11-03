// vim: ts=2:sw=2:expandtab:
/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Android Sync Client.
 *
 * The Initial Developer of the Original Code is
 * the Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Jason Voll <jvoll@mozilla.com>
 * Richard Newman <rnewman@mozilla.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.android.sync;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.mozilla.android.sync.domain.CryptoInfo;
import org.mozilla.android.sync.domain.CryptoStatusBundle;
import org.mozilla.android.sync.domain.CryptoStatusBundle.CryptoStatus;
import org.mozilla.android.sync.domain.KeyBundle;

/*
 * This class acts as a wrapper for the Cryptographer class.
 * The goal here is to take care of all JSON parsing and BaseXX
 * encoding/decoding so that the Cryptographer class doesn't need
 * to know anything about the format of the original data. It also
 * enables classes to get crypto services based on a WBO, rather than
 * having to parse JSON themselves.
 *
 * This class decouples the Cryptographer from the Sync client.
 */
public class SyncCryptographer {

  // JSON related constants.
  private static final String KEY_CIPHER_TEXT =        "ciphertext";
  private static final String KEY_HMAC =               "hmac";
  private static final String KEY_IV =                 "IV";
  private static final String KEY_PAYLOAD =            "payload";
  private static final String KEY_ID =                 "id";
  private static final String KEY_COLLECTION =         "collection";
  private static final String KEY_COLLECTIONS =        "collections";
  private static final String KEY_DEFAULT_COLLECTION = "default";

  private static final String ID_CRYPTO_KEYS =         "keys";
  private static final String CRYPTO_KEYS_COLLECTION = "crypto";

  private byte[] syncKey;
  private String username;
  private KeyBundle keys;

  /*
   * Constructors.
   */
  public SyncCryptographer(String username) {
    this(username, "", "", "");
  }

  public SyncCryptographer(String username, String friendlyBase32SyncKey) {
    this(username, friendlyBase32SyncKey, "", "");
  }

  public SyncCryptographer(String username, String friendlyBase32SyncKey,
                           String base64EncryptionKey, String base64HmacKey) {
    this.setUsername(username);
    this.setSyncKey(friendlyBase32SyncKey);
    this.setKeys(base64EncryptionKey, base64HmacKey);
  }

  /*
   * Input: A string representation of a WBO (JSON) payload to be encrypted.
   * Output:  CryptoStatusBundle with a JSON payload containing
   *          crypto information (ciphertext, IV, HMAC).
   */
  public CryptoStatusBundle encryptWBO(String jsonString) {
    // Verify that encryption keys are set.
    if (keys == null) {
      return new CryptoStatusBundle(CryptoStatus.MISSING_KEYS, jsonString);
    }
    return encrypt(jsonString, keys);
  }

  /*
   * Input: A string representation of the WBO (JSON).
   * Output: the decrypted payload and status.
   */
  public CryptoStatusBundle decryptWBO(String jsonString) {
    // Get JSON from string.
    JSONObject json = null;
    JSONObject payload = null;
    try {
      json = getJSONObject(jsonString);
      payload = getJSONObject(json, KEY_PAYLOAD);

    } catch (Exception e) {
      return new CryptoStatusBundle(CryptoStatus.INVALID_JSON, jsonString);
    }

    // Check that payload contains all pieces for crypto.
    if (!payload.containsKey(KEY_CIPHER_TEXT) ||
        !payload.containsKey(KEY_IV) ||
        !payload.containsKey(KEY_HMAC)) {
      return new CryptoStatusBundle(CryptoStatus.INVALID_JSON, jsonString);
    }

    String id = (String) json.get(KEY_ID);
    if (id.equalsIgnoreCase(ID_CRYPTO_KEYS)) {
      // If this is a crypto keys bundle, handle it separately.
      return decryptKeysWBO(payload);
    } else if (keys == null) {
      // Otherwise, make sure we have crypto keys before continuing.
      return new CryptoStatusBundle(CryptoStatus.MISSING_KEYS, jsonString);
    }

    byte[] clearText = decryptPayload(payload, this.keys);
    return new CryptoStatusBundle(CryptoStatus.OK, new String(clearText));
  }

  /*
   * Handles the case where we are decrypting the crypto/keys bundle.
   * Uses the sync key and username to get keys for decrypting this
   * bundle. Once bundle is decrypted the keys are saved to this
   * object for future use and the decrypted payload is returned.
   *
   * Input: JSONObject payload containing crypto/keys JSON.
   * Output: Decrypted crypto/keys String.
   */
  private CryptoStatusBundle decryptKeysWBO(JSONObject payload) {
    // Get the keys to decrypt the crypto keys bundle.
    KeyBundle cryptoKeysBundleKeys;
    try {
      cryptoKeysBundleKeys = getCryptoKeysBundleKeys();
    } catch (Exception e) {
      return new CryptoStatusBundle(CryptoStatus.MISSING_SYNCKEY_OR_USER, payload.toString());
    }

    byte[] cryptoKeysBundle = decryptPayload(payload, cryptoKeysBundleKeys);

    // Extract decrypted keys.
    InputStream stream = new ByteArrayInputStream(cryptoKeysBundle);
    Reader in = new InputStreamReader(stream);
    JSONObject json = null;
    try {
      json = (JSONObject) new JSONParser().parse(in);
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Verify that this is indeed the crypto/keys bundle and that
    // decryption worked.
    String id = (String) json.get(KEY_ID);
    String collection = (String) json.get(KEY_COLLECTION);

    if (id.equalsIgnoreCase(ID_CRYPTO_KEYS) &&
        collection.equalsIgnoreCase(CRYPTO_KEYS_COLLECTION) &&
        json.containsKey(KEY_DEFAULT_COLLECTION)) {

      // Extract the keys and add them to this.
      Object jsonKeysObj = json.get(KEY_DEFAULT_COLLECTION);
      if (jsonKeysObj.getClass() != JSONArray.class) {
        return new CryptoStatusBundle(CryptoStatus.INVALID_KEYS_BUNDLE,
                                      json.toString());
      }

      JSONArray jsonKeys = (JSONArray) jsonKeysObj;
      this.setKeys((String) jsonKeys.get(0), (String) jsonKeys.get(1));

      // Return the string containing the decrypted payload.
      return new CryptoStatusBundle(CryptoStatus.OK,
                                    new String(cryptoKeysBundle));
    } else {
      return new CryptoStatusBundle(CryptoStatus.INVALID_KEYS_BUNDLE,
                                    json.toString());
    }
  }

  /**
   * Generates new encryption keys and creates the crypto/keys
   * payload, encrypted using Sync Key.
   *
   * @return The crypto/keys payload encrypted for sending to
   * the server.
   */
  public CryptoStatusBundle generateCryptoKeysWBOPayload() {

    // Generate the keys and save for later use.
    KeyBundle cryptoKeys = Cryptographer.generateKeys();
    setKeys(Base64.encodeBase64String(cryptoKeys.getEncryptionKey()),
        Base64.encodeBase64String(cryptoKeys.getHmacKey()));

    // Generate JSON.
    JSONArray keysArray = new JSONArray();
    Utils.asAList(keysArray).add(new String(Base64.encodeBase64(cryptoKeys.getEncryptionKey())));
    Utils.asAList(keysArray).add(new String(Base64.encodeBase64(cryptoKeys.getHmacKey())));
    JSONObject json = new JSONObject();
    Utils.asMap(json).put(KEY_ID, ID_CRYPTO_KEYS);
    Utils.asMap(json).put(KEY_COLLECTION, CRYPTO_KEYS_COLLECTION);
    Utils.asMap(json).put(KEY_COLLECTIONS, "{}");
    Utils.asMap(json).put(KEY_DEFAULT_COLLECTION, keysArray);

    // Get the keys to encrypt the crypto/keys bundle.
    KeyBundle cryptoKeysBundleKeys;
    try {
      cryptoKeysBundleKeys = getCryptoKeysBundleKeys();
    } catch (Exception e) {
      return new CryptoStatusBundle(CryptoStatus.MISSING_SYNCKEY_OR_USER, "");
    }

    return encrypt(json.toString(), cryptoKeysBundleKeys);
  }

  /////////////////////// HELPERS /////////////////////////////

  /*
   * Helper method for doing actual encryption.
   *
   * Input:   Message to encrypt, Keys for encryption/hmac
   * Output:  CryptoStatusBundle with a JSON payload containing
   *          crypto information (ciphertext, iv, hmac).
   */
  private CryptoStatusBundle encrypt(String message, KeyBundle keys) {
    CryptoInfo encrypted = Cryptographer.encrypt(new CryptoInfo(message.getBytes(), keys));
    String payload = createJSONBundle(encrypted);
    return new CryptoStatusBundle(CryptoStatus.OK, payload);
  }

  /**
   * Helper method for doing actual decryption.
   *
   * Input:   JSONObject containing a valid payload (cipherText, IV, HMAC),
   *          KeyBundle with keys for decryption.
   * Output:  byte[] clearText
   */
  private byte[] decryptPayload(JSONObject payload, KeyBundle keybundle) {
    byte[] clearText = Cryptographer.decrypt(
      new CryptoInfo (
        Base64.decodeBase64( (String) payload.get(KEY_CIPHER_TEXT) ),
        Base64.decodeBase64( (String) payload.get(KEY_IV) ),
        Utils.hex2Byte( (String) payload.get(KEY_HMAC) ),
        keybundle
        )
      );

    return clearText;
  }

  /**
   * Helper method to get a JSONObject from a String.
   * Input:   String containing JSON.
   * Output:  Extracted JSONObject.
   * Throws:  Exception if JSON is invalid.
   */
  private JSONObject getJSONObject(String jsonString) throws Exception {
    Reader in = new StringReader(jsonString);
    try {
      return (JSONObject) new JSONParser().parse(in);
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Helper method for extracting a JSONObject
   * from within another JSONObject.
   *
   * Input:   JSONObject and key.
   * Output:  JSONObject extracted.
   * Throws:  Exception if JSON is invalid.
   */
  private JSONObject getJSONObject(JSONObject json, String key) throws Exception {
    try {
      return getJSONObject((String) json.get(key));
    } catch (Exception e) {
      throw e;
    }
  }

  /*
   * Helper to create JSON bundle for encrypted objects.
   */
  private String createJSONBundle(CryptoInfo info) {
    JSONObject json = new JSONObject();
    Utils.asMap(json).put(KEY_CIPHER_TEXT, new String(Base64.encodeBase64(info.getMessage())));
    Utils.asMap(json).put(KEY_HMAC, Utils.byte2hex(info.getHmac()));
    Utils.asMap(json).put(KEY_IV, new String(Base64.encodeBase64(info.getIv())));
    return json.toString();
  }

  /*
   * Get the keys needed to encrypt the crypto/keys bundle.
   *
   * Throws:  Exception if syncKey or username is not set.
   */
  public KeyBundle getCryptoKeysBundleKeys() throws Exception {
    // Check that we have the sync key and username
    if (syncKey == null || username.equalsIgnoreCase("")) {
      throw new Exception();
    }

    byte[][] keys = HKDF.getCryptoKeysBundleKeys(syncKey, username.getBytes());
    return new KeyBundle(keys[0], keys[1]);
  }

  /*
   * Accessors/Mutators.
   */
  public byte[] getSyncKey() {
    return syncKey;
  }

  /*
   * Input: FriendlyBase32 encoded sync key.
   */
  public void setSyncKey(String friendlyBase32SyncKey) {
    this.syncKey = Utils.decodeFriendlyBase32(friendlyBase32SyncKey);
  }

  public KeyBundle getKeys() {
    return keys;
  }

  /*
   * Input: Base64 encoded encryption and HMAC keys.
   */
  public void setKeys(String base64EncryptionKey, String base64HmacKey) {
    this.keys = new KeyBundle(Base64.decodeBase64(base64EncryptionKey),
                              Base64.decodeBase64(base64HmacKey));
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }
}
