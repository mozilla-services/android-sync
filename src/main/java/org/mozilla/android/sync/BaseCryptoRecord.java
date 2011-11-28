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
   * @throws UnsupportedEncodingException 
   */
  private static byte[] decryptPayload(ExtendedJSONObject payload, KeyBundle keybundle) throws CryptoException, UnsupportedEncodingException {
    byte[] ciphertext = Base64.decodeBase64(((String) payload.get(KEY_CIPHERTEXT)).getBytes("UTF-8"));
    byte[] iv         = Base64.decodeBase64(((String) payload.get(KEY_IV)).getBytes("UTF-8"));
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
  public CryptoRecord decrypt() throws CryptoException, IOException, ParseException,
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
    return this;
  }

  @Override
  public CryptoRecord encrypt() throws CryptoException, UnsupportedEncodingException {
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
    return this;
  }
}
