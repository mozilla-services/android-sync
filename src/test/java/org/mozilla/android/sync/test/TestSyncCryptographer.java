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
}
