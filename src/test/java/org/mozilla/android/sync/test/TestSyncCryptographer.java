/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.mozilla.android.sync.domain.CryptoStatusBundle;
import org.mozilla.android.sync.domain.KeyBundle;
import org.mozilla.android.sync.domain.CryptoStatusBundle.CryptoStatus;
import org.mozilla.android.sync.CryptoException;
import org.mozilla.android.sync.SyncCryptographer;
import org.mozilla.android.sync.Utils;

public class TestSyncCryptographer {

    @Test
    public void testDecrypt() throws CryptoException {
        String jsonInput =              "{\"sortindex\": 90, \"payload\":" +
                                        "\"{\\\"ciphertext\\\":\\\"F4ukf0" +
                                        "LM+vhffiKyjaANXeUhfmOPPmQYX1XBoG" +
                                        "Rh1LiHeKHB5rqjhzd7yAoxqgmFnkIgQF" +
                                        "YPSqRAoCxWiAeGULTX+KM4MU5drbNyR/" +
                                        "690JBWSyE1vQSiMGwNIbTKnOLGHKkQVY" +
                                        "HDpajg5BNFfvHNQ5Jx7uM9uJcmuEjCI6" +
                                        "GRMDKyKjhsTqCd99MONkY5rISutaWQ0e" +
                                        "EXFgpA9RZPv4jgWlQhe+YrVnpcrTi20b" +
                                        "NgKp3IfIeqEelrZ5FJd2WGZOA021d3e7" +
                                        "P3Z4qptefH4Q9/hySrWsELWngBaydyn/" +
                                        "IjsheZuKra3kJSST/4SvRZ7qXn\\\",\\" +
                                        "\"IV\\\":\\\"GadPajeXhpk75K2YH+L" +
                                        "y4w==\\\",\\\"hmac\\\":\\\"71442" +
                                        "d946502e3ca475c70a633d3d37f4b4e9" +
                                        "313a6d1041d0c0550cd354e7605\\\"}" +
                                        "\", \"id\": \"hkZYpC-BH4Xi\", \"" +
                                        "modified\": 1320183464.21}";
        String base64EncryptionKey =    "K8fV6PHG8RgugfHexGesbzTeOs2o12cr" +
        		                        "N/G3bz0Bx1M=";
        String base64HmacKey =          "nbceuI6w1RJbBzh+iCJHEs8p4lElsOma" +
        		                        "yUhx+OztVgM=";
        String username =               "b6evr62dptbxz7fvebek7btljyu322wp";
        String friendlyBase32SyncKey =  "basuxv2426eqj7frhvpcwkavdi";
        String expectedDecryptedText =  "{\"id\":\"hkZYpC-BH4Xi\",\"histU" +
                                        "ri\":\"http://hathology.com/2008" +
                                        "/06/how-to-edit-your-path-enviro" +
                                        "nment-variables-on-mac-os-x/\",\"" +
                                        "title\":\"How To Edit Your PATH " +
                                        "Environment Variables On Mac OS " +
                                        "X\",\"visits\":[{\"date\":131898" +
                                        "2074310889,\"type\":1}]}";

        SyncCryptographer cryptographer = new SyncCryptographer(username, friendlyBase32SyncKey, base64EncryptionKey, base64HmacKey);
        CryptoStatusBundle result = cryptographer.decryptWBO(jsonInput);

        // Check result
        assertEquals(CryptoStatus.OK, result.getStatus());
        assertEquals(expectedDecryptedText, result.getJson());

    }

    @Test
    public void testEncryptDecrypt() throws CryptoException {
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
        JSONObject in = new JSONObject();
        Utils.asMap(in).put("payload", result.getJson());
        Utils.asMap(in).put("id", "garbage_key");

        String jsonString = in.toString();
        result = cryptographer.decryptWBO(jsonString);

        // Check that decrypted text matches original
        assertEquals(originalText, result.getJson());

        // Check the status to make sure it worked
        assertEquals(CryptoStatus.OK, result.getStatus());
    }

    @Test
    public void testConstructKeyBundleKeys() {

        String username =               "smqvooxj664hmrkrv6bw4r4vkegjhkns";
        String friendlyBase32SyncKey =  "gbh7teqqcgyzd65svjgibd7tqy";
        String base64EncryptionKey =    "069EnS3EtDK4y1tZ1AyKX+U7WEsWRp9b" +
                                        "RIKLdW/7aoE=";
        String base64HmacKey =          "LF2YCS1QCgSNCf0BCQvQ06SGH8jqJDi9" +
                                        "dKj0O+b0fwI=";

        SyncCryptographer cryptographer = new SyncCryptographer(username, friendlyBase32SyncKey);
        KeyBundle keys;
        try {
            keys = cryptographer.getCryptoKeysBundleKeys();
        } catch (Exception e) {
            fail();
            return;
        }

        // Check Encryption Key
        boolean equal = Arrays.equals(keys.getEncryptionKey(), Base64.decodeBase64(base64EncryptionKey));
        assertEquals(true, equal);

        // Check HMAC Key
        equal = Arrays.equals(keys.getHmacKey(), Base64.decodeBase64(base64HmacKey));
        assertEquals(true, equal);

    }

    @Test
    public void testDecryptKeysBundle() throws CryptoException {
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
        boolean equal = Arrays.equals(Base64.decodeBase64(expectedBase64EncryptionKey), cryptographer.getKeys().getEncryptionKey());
        assertEquals(true, equal);
        equal = Arrays.equals(Base64.decodeBase64(expectedBase64HmacKey), cryptographer.getKeys().getHmacKey());
        assertEquals(true, equal);

        // Check the decrypted text
        assertEquals(expectedDecryptedText, result.getJson());
    }

    @Test
    public void testCreateKeysBundle() throws CryptoException {
        String username =                       "b6evr62dptbxz7fvebek7btljyu322wp";
        String friendlyBase32SyncKey =          "basuxv2426eqj7frhvpcwkavdi";

        // Generate keys
        SyncCryptographer cryptographer = new SyncCryptographer(username, friendlyBase32SyncKey);
        CryptoStatusBundle result = cryptographer.generateCryptoKeysWBOPayload();

        // Check result status and get generated keys
        assertEquals(CryptoStatus.OK, result.getStatus());
        KeyBundle createdKeys = cryptographer.getKeys();

        // Decrypt what we created to see the keys as they were stored in the json
        JSONObject in = new JSONObject();
        Utils.asMap(in).put("payload", result.getJson());
        Utils.asMap(in).put("id", "keys");
        result = cryptographer.decryptWBO(in.toString());

        // Check result and get decrypted keys
        KeyBundle decryptedKeys = cryptographer.getKeys();
        assertEquals(CryptoStatus.OK, result.getStatus());

        // Compare decrypted keys to the keys that were set upon creation
        boolean equal = Arrays.equals(createdKeys.getEncryptionKey(), decryptedKeys.getEncryptionKey());
        assertEquals(true, equal);
        equal = Arrays.equals(createdKeys.getHmacKey(), decryptedKeys.getHmacKey());
        assertEquals(true, equal);
    }

}
