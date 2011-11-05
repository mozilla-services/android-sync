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

package org.mozilla.android.sync;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.mozilla.android.sync.domain.CryptoInfo;
import org.mozilla.android.sync.domain.KeyBundle;

/*
 * Implements the basic cryptography options required
 */
public class Cryptographer {

    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM_SPEC = "AES";
    private static final int KEY_SIZE = 256;

    public static CryptoInfo encrypt(CryptoInfo info) {

        Cipher cipher = getCipher();
        try {
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(info.getKeys().getEncryptionKey(),
                        KEY_ALGORITHM_SPEC));
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }

        // Encrypt
        byte[] encryptedBytes = commonCrypto(cipher, info.getMessage());
        info.setMessage(encryptedBytes);

        // Save IV
        info.setIv(cipher.getIV());

        // Generate HMAC
        info.setHmac(generateHmac(info));

        return info;

    }

    /*
     * Perform a decryption
     *
     * Input: info bundle for decryption
     * Ouput: decrypted byte[]
     */
    public static byte[] decrypt(CryptoInfo info) {

        // Check HMAC
        if (!verifyHmac(info)) {
            return null;
        }

        Cipher cipher = getCipher();
        try {
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(info.getKeys().getEncryptionKey(),
                        KEY_ALGORITHM_SPEC), new IvParameterSpec(info.getIv()));
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return null;
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            return null;
        }

        return commonCrypto(cipher, info.getMessage());
    }

    /*
     * Make 2 random 256 bit keys (encryption and hmac)
     */
    public static KeyBundle generateKeys() {
        KeyGenerator keygen;
        try {
            keygen = KeyGenerator.getInstance(KEY_ALGORITHM_SPEC);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

        keygen.init(KEY_SIZE);
        byte[] encryptionKey = keygen.generateKey().getEncoded();
        byte[] hmacKey = keygen.generateKey().getEncoded();
        return new KeyBundle(encryptionKey, hmacKey);
    }

    /*
     * Performs functionality common to both the
     * encryption and decryption operations
     *
     * Input: Cipher object, non-BaseXX-encoded byte[] input
     * Output: encrypted/decrypted byte[]
     */
    private static byte[] commonCrypto(Cipher cipher, byte[] inputMessage) {
        byte[] outputMessage = null;
        try {
            outputMessage = cipher.doFinal(inputMessage);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return outputMessage;
    }

    /*
     * Helper to get a Cipher object
     * Input: None
     * Output: Cipher object
     */
    private static Cipher getCipher() {
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance(TRANSFORMATION);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        return cipher;
    }

    /*
     * Helper to verify HMAC
     * Input: CyrptoInfo
     * Output: true if HMAC is correct
     */
    private static boolean verifyHmac(CryptoInfo bundle) {
        return Arrays.equals(generateHmac(bundle), bundle.getHmac());
    }

    /*
     * Helper to generate HMAC
     * Input: CryptoInfo
     * Output: a generated HMAC for given cipher text
     */
    private static byte[] generateHmac(CryptoInfo bundle) {
        Mac hmacHasher = HKDF.makeHmacHasher(HKDF.makeHmacKey(bundle.getKeys().getHmacKey()));
        return hmacHasher.doFinal(Base64.encodeBase64(bundle.getMessage()));
    }

}
