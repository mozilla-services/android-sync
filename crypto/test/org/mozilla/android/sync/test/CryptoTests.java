/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.mozilla.android.sync.CryptoInfo;
import org.mozilla.android.sync.Cryptographer;
import org.mozilla.android.sync.HKDF;
import org.mozilla.android.sync.KeyBundle;
import org.mozilla.android.sync.Utils;

/*
 * Note: Currently all tests are based on the fact that I get clear text
 * when I decrypt...these aren't taken from other Mozilla crypto libraries
 * where we know that the results are right...they are currently just sanity
 * tests by which I assume that since I get the correct clear text and HMAC's
 * check out, things are mostly correct. For now this is good enough, later,
 * not so much.
 */
public class CryptoTests {

    // TODO why am I doing byte conversion where I am when the Base64 methods can handle strings anyways!!!
    
    @Test
    public void testConstructKeyBundleKeys() {
        
        String username =               "smqvooxj664hmrkrv6bw4r4vkegjhkns";
        String friendlyBase32SyncKey =  "gbh7teqqcgyzd65svjgibd7tqy";
        String base64EncryptionKey =    "069EnS3EtDK4y1tZ1AyKX+U7WEsWRp9bRIKLdW/7aoE=";
        String base64HmacKey =          "LF2YCS1QCgSNCf0BCQvQ06SGH8jqJDi9dKj0O+b0fwI=";
        
        KeyBundle keys = Cryptographer.getCryptoKeysBundleKeys
                (Utils.decodeFriendlyBase32(friendlyBase32SyncKey), username.getBytes());
        
        boolean equal;
        // Check Encryption Key
        equal = Arrays.equals(keys.getEncryptionKey(), Base64.decodeBase64(base64EncryptionKey));
        assertEquals(true, equal);
        
        // Check HMAC Key
        equal = Arrays.equals(keys.getHmacKey(), Base64.decodeBase64(base64HmacKey));
        assertEquals(true, equal);
        
    }
    
    @Test
    public void testExtractCryptoKeys() {
        String username =                       "smqvooxj664hmrkrv6bw4r4vkegjhkns";
        String friendlyBase32SyncKey =          "gbh7teqqcgyzd65svjgibd7tqy";
        byte[] base64CipherText =               "lBsYDi/UPX/PwIdAUaBGaob2J6O3YmAEEWC4l4oD/aZajQ38zxp7UH9gNNeZ9oy3lMWCtcrDKM+EXWhQSB+Jfbl3fcKdaFP+8MbkxbFXAY/hNTiTq9XB9PxKJZnDte2i/uIa3Thy4jbU7eVHMxWL1s0Z6G+H7qiQBJIVRDuCehn3zeM0bNRcj6RJMnLMmd2/tn6qTIxwyT74sqpcTSVxhA==".getBytes();
        byte[] base64IV =                       "RlBJdQcp6mPWKmCRisUrtQ==".getBytes();
        String base16Hmac =                     "aa5fc1ba11bb4ef7660046ea285c93a9bbed2805a101d3a02b9301a1c3f852e1";
        byte[] expectedBase64EncryptionKey =    "9K/wLdXdw+nrTtXo4ZpECyHFNr4d7aYHqeg3KW9+m6Q=".getBytes();
        byte[] expectedBase64HmacKey =          "MMntEfutgLTc8FlTLQFms8/xMPmCldqPlq/QQXEjx70=".getBytes();
                
        KeyBundle actualKeys = Cryptographer.extractCryptoKeys(Utils.decodeFriendlyBase32(friendlyBase32SyncKey), username.getBytes(), 
                new CryptoInfo(Base64.decodeBase64(base64CipherText), Base64.decodeBase64(base64IV), Utils.hex2Byte(base16Hmac), null));
        
        
        // Note...need to verify that the values we are getting are actually correct and decrypt stuff, but they look good :)
        boolean equal;
        // Check Encryption Key
        equal = Arrays.equals(actualKeys.getEncryptionKey(), Base64.decodeBase64(expectedBase64EncryptionKey));
        assertEquals(true, equal);
        
        // Check HMAC Key
        equal = Arrays.equals(actualKeys.getHmacKey(), Base64.decodeBase64(expectedBase64HmacKey));
        assertEquals(true, equal);
        
        
    }
    
    @Test
    public void testHmacVerification() {
        
        // Test 2 cases - 1 match, 1 doesn't
    }
    
    
    @Test
    public void testDecrypt() {

        String base64CipherText =       "NMsdnRulLwQsVcwxKW9XwaUe7ouJk5Wn" +
                                        "80QhbD80l0HEcZGCynh45qIbeYBik0lg" +
                                        "cHbKmlIxTJNwU+OeqipN+/j7MqhjKOGI" +
                                        "lvbpiPQQLC6/ffF2vbzL0nzMUuSyvaQz" +
                                        "yGGkSYM2xUFt06aNivoQTvU2GgGmUK6M" +
                                        "vadoY38hhW2LCMkoZcNfgCqJ26lO1O0s" +
                                        "EO6zHsk3IVz6vsKiJ2Hq6VCo7hu123wN" +
                                        "egmujHWQSGyf8JeudZjKzfi0OFRRvvm4" +
                                        "QAKyBWf0MgrW1F8SFDnVfkq8amCB7Nhd" +
                                        "whgLWbN+21NitNwWYknoEWe1m6hmGZDg" +
                                        "DT32uxzWxCV8QqqrpH/ZggViEr9uMgoy" +
                                        "4lYaWqP7G5WKvvechc62aqnsNEYhH26A" +
                                        "5QgzmlNyvB+KPFvPsYzxDnSCjOoRSLx7" +
                                        "GG86wT59QZw=";
        String base64IV =               "GX8L37AAb2FZJMzIoXlX8w==";
        String base16Hmac =             "b1e6c18ac30deb70236bc0d65a46f7a4" +
                                        "dce3b8b0e02cf92182b914e3afa5eebc";
        String base64EncryptionKey =    "9K/wLdXdw+nrTtXo4ZpECyHFNr4d7aYH" +
                                        "qeg3KW9+m6Q=";
        String base64HmacKey =          "MMntEfutgLTc8FlTLQFms8/xMPmCldqP" +
                                        "lq/QQXEjx70=";
        String base64ExpectedBytes =    "eyJpZCI6IjVxUnNnWFdSSlpYciIsImhp" +
                                        "c3RVcmkiOiJmaWxlOi8vL1VzZXJzL2ph" +
                                        "c29uL0xpYnJhcnkvQXBwbGljYXRpb24l" +
                                        "MjBTdXBwb3J0L0ZpcmVmb3gvUHJvZmls" +
                                        "ZXMva3NnZDd3cGsuTG9jYWxTeW5jU2Vy" +
                                        "dmVyL3dlYXZlL2xvZ3MvIiwidGl0bGUi" +
                                        "OiJJbmRleCBvZiBmaWxlOi8vL1VzZXJz" +
                                        "L2phc29uL0xpYnJhcnkvQXBwbGljYXRp" +
                                        "b24gU3VwcG9ydC9GaXJlZm94L1Byb2Zp" +
                                        "bGVzL2tzZ2Q3d3BrLkxvY2FsU3luY1Nl" +
                                        "cnZlci93ZWF2ZS9sb2dzLyIsInZpc2l0" +
                                        "cyI6W3siZGF0ZSI6MTMxOTE0OTAxMjM3" +
                                        "MjQyNSwidHlwZSI6MX1dfQ==";
        
        
        byte[] decodedBytes = Cryptographer.decrypt(
                new CryptoInfo(
                    Base64.decodeBase64(base64CipherText),
                    Base64.decodeBase64(base64IV),
                    Utils.hex2Byte(base16Hmac),
                    new KeyBundle(
                            Base64.decodeBase64(base64EncryptionKey),
                            Base64.decodeBase64(base64HmacKey))
                ));
        
        // Check result
        boolean equals = Arrays.equals(decodedBytes, Base64.decodeBase64(base64ExpectedBytes));
        assertEquals(true, equals);
        
    }

}
