/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.junit.Test;
import org.mozilla.apache.commons.codec.binary.Base64;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.cryptographer.CryptoStatusBundle;
import org.mozilla.gecko.sync.cryptographer.CryptoStatusBundle.CryptoStatus;
import org.mozilla.gecko.sync.cryptographer.SyncCryptographer;

public class TestSyncCryptographer {

    @Test
    public void testEncryptDecrypt() throws CryptoException, UnsupportedEncodingException {
        String originalText =           "{\"id\":\"hkZYpC-BH4Xi\",\"histU" +
                                        "ri\":\"http://hathology.com/2008" +
                                        "/06/how-to-edit-your-path-enviro" +
                                        "nment-variables-on-mac-os-x/\",\"" +
                                        "title\":\"How To Edit Your PATH " +
                                        "Environment Variables On Mac OS " +
                                        "X\",\"visits\":[{\"date\":131898" +
                                        "2074310889,\"type\":1}]}";
        String base64EncryptionKey =    "K8fV6PHG8RgugfHexGesbzTeOs2o12cr" +
                                        "N/G3bz0Bx1M=";
        String base64HmacKey =          "nbceuI6w1RJbBzh+iCJHEs8p4lElsOma" +
                                        "yUhx+OztVgM=";
        String username =               "b6evr62dptbxz7fvebek7btljyu322wp";
        String friendlyBase32SyncKey =  "basuxv2426eqj7frhvpcwkavdi";

        // Encrypt
        SyncCryptographer cryptographer = new SyncCryptographer(username, friendlyBase32SyncKey, base64EncryptionKey, base64HmacKey);
        CryptoStatusBundle result = cryptographer.encryptWBO(originalText);

        // Check the status to make sure it worked
        assertEquals(CryptoStatus.OK, result.getStatus());

        // Decrypt
        ExtendedJSONObject in = new ExtendedJSONObject();
        in.put("payload", result.getJson());
        in.put("id", "garbage_key");

        String jsonString = in.toJSONString();
        result = cryptographer.decryptWBO(jsonString);

        // Check that decrypted text matches original
        assertEquals(originalText, result.getJson());

        // Check the status to make sure it worked
        assertEquals(CryptoStatus.OK, result.getStatus());
    }

    @Test
    public void testDecryptKeysBundle() throws CryptoException, UnsupportedEncodingException {
        String jsonInput =                      "{\"payload\": \"{\\\"ciphertext\\" +
                                                "\":\\\"L1yRyZBkVYKXC1cTpeUqqfmKg" +
                                                "CinYV9YntGiG0PfYZSTLQ2s86WPI0VBb" +
                                                "QbLZfx7udk6sf6CFE4w5EgiPx0XP3Fbj" +
                                                "L7r4qIT0vjbAOrLKedZwA3cgiquc+PXM" +
                                                "Etml8B4Dfm0crJK0iROlRkb+lePAYkzI" +
                                                "iQn5Ba8mSWQEFoLy3zAcfCYXumA7E0Fj" +
                                                "XYD+TqTG5bqYJY4zvPaB9mn9y3WHw==\\" +
                                                "\",\\\"IV\\\":\\\"Jjb2oVI5uvvFfm" +
                                                "ZYRY4GaA==\\\",\\\"hmac\\\":\\\"" +
                                                "0b59731cb1aaedc85f54917b7058f361" +
                                                "60826b70050b0d70cd42b0b609b1d717" +
                                                "\\\"}\", \"id\": \"keys\", \"mod" +
                                                "ified\": 1320183463.91}";
        String username =                       "b6evr62dptbxz7fvebek7btljyu322wp";
        String friendlyBase32SyncKey =          "basuxv2426eqj7frhvpcwkavdi";
        String expectedDecryptedText =          "{\"default\":[\"K8fV6PHG8RgugfHe" +
                                                "xGesbzTeOs2o12crN/G3bz0Bx1M=\",\"" +
                                                "nbceuI6w1RJbBzh+iCJHEs8p4lElsOma" +
                                                "yUhx+OztVgM=\"],\"collections\":" +
                                                "{},\"collection\":\"crypto\",\"i" +
                                                "d\":\"keys\"}";
        String expectedBase64EncryptionKey =    "K8fV6PHG8RgugfHexGesbzTeOs2o12cr" +
                                                "N/G3bz0Bx1M=";
        String expectedBase64HmacKey =          "nbceuI6w1RJbBzh+iCJHEs8p4lElsOma" +
                                                "yUhx+OztVgM=";


        SyncCryptographer cryptographer = new SyncCryptographer(username, friendlyBase32SyncKey);
        CryptoStatusBundle result = cryptographer.decryptWBO(jsonInput);

        // Check result status
        assertEquals(CryptoStatus.OK, result.getStatus());

        // Check that the correct keys were set and are not base64 encoded
        boolean equal = Arrays.equals(Base64.decodeBase64(expectedBase64EncryptionKey.getBytes("UTF-8")), cryptographer.getKeys().getEncryptionKey());
        assertEquals(true, equal);
        equal = Arrays.equals(Base64.decodeBase64(expectedBase64HmacKey.getBytes("UTF-8")), cryptographer.getKeys().getHMACKey());
        assertEquals(true, equal);

        // Check the decrypted text
        assertEquals(expectedDecryptedText, result.getJson());
    }

    @Test
    public void testCreateKeysBundle() throws CryptoException, UnsupportedEncodingException {
        String username =                       "b6evr62dptbxz7fvebek7btljyu322wp";
        String friendlyBase32SyncKey =          "basuxv2426eqj7frhvpcwkavdi";

        // Generate keys
        SyncCryptographer cryptographer = new SyncCryptographer(username, friendlyBase32SyncKey);
        CryptoStatusBundle result = cryptographer.generateCryptoKeysWBOPayload();

        // Check result status and get generated keys
        assertEquals(CryptoStatus.OK, result.getStatus());
        KeyBundle createdKeys = cryptographer.getKeys();

        // Decrypt what we created to see the keys as they were stored in the json
        ExtendedJSONObject in = new ExtendedJSONObject();
        in.put("payload", result.getJson());
        in.put("id", "keys");
        result = cryptographer.decryptWBO(in.toJSONString());

        // Check result and get decrypted keys
        KeyBundle decryptedKeys = cryptographer.getKeys();
        assertEquals(CryptoStatus.OK, result.getStatus());

        // Compare decrypted keys to the keys that were set upon creation
        boolean equal = Arrays.equals(createdKeys.getEncryptionKey(), decryptedKeys.getEncryptionKey());
        assertEquals(true, equal);
        equal = Arrays.equals(createdKeys.getHMACKey(), decryptedKeys.getHMACKey());
        assertEquals(true, equal);
    }
}
