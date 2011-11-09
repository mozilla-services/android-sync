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
 * Jason Voll
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

package org.mozilla.android.sync.domain;

import javax.crypto.Mac;

import org.mozilla.android.sync.HKDF;
import org.mozilla.android.sync.Utils;

public class KeyBundle {

    private byte[] encryptionKey;
    private byte[] hmacKey;

    // These are the same for every sync key bundle.
    private static final byte[] EMPTY_BYTES      = {};
    private static final byte[] ENCR_INPUT_BYTES = {1};
    private static final byte[] HMAC_INPUT_BYTES = {2};

    /*
     * Mozilla's use of HKDF for getting keys from the Sync Key string.
     *
     * We do exactly 2 HKDF iterations and make the first iteration the
     * encryption key and the second iteration the HMAC key.
     *
     */
    public KeyBundle(String username, String base32SyncKey) {
      if (base32SyncKey == null) {
        throw new IllegalArgumentException("No sync key provided.");
      }
      if (username == null || username.equals("")) {
        throw new IllegalArgumentException("No username provided.");
      }
      byte[] syncKey = Utils.decodeFriendlyBase32(base32SyncKey);
      byte[] user    = username.getBytes();

      Mac hmacHasher = HKDF.makeHMACHasher(syncKey);

      byte[] encrBytes = Utils.concatAll(EMPTY_BYTES, HKDF.HMAC_INPUT, user, ENCR_INPUT_BYTES);
      byte[] encrKey   = HKDF.digestBytes(encrBytes, hmacHasher);
      byte[] hmacBytes = Utils.concatAll(encrKey, HKDF.HMAC_INPUT, user, HMAC_INPUT_BYTES);

      this.hmacKey       = HKDF.digestBytes(hmacBytes, hmacHasher);
      this.encryptionKey = encrKey;
    }

    public KeyBundle(byte[] encryptionKey, byte[] hmacKey) {
       this.setEncryptionKey(encryptionKey);
       this.setHMACKey(hmacKey);
    }

    public byte[] getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(byte[] encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public byte[] getHMACKey() {
        return hmacKey;
    }

    public void setHMACKey(byte[] hmacKey) {
        this.hmacKey = hmacKey;
    }

}
