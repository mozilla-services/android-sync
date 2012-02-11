/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.junit.Test;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.cryptographer.CryptoStatusBundle;
import org.mozilla.gecko.sync.cryptographer.CryptoStatusBundle.CryptoStatus;
import org.mozilla.gecko.sync.cryptographer.SyncCryptographer;

public class TestSyncCryptographer {

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
