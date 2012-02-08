/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.mozilla.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.cryptographer.CryptoStatusBundle;
import org.mozilla.gecko.sync.cryptographer.CryptoStatusBundle.CryptoStatus;
import org.mozilla.gecko.sync.cryptographer.SyncCryptographer;

public class TestSyncCryptographer {

    @Test
    public void testDecrypt() throws CryptoException, UnsupportedEncodingException {
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
