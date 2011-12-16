package org.mozilla.android.sync.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.mozilla.android.sync.crypto.CryptoException;
import org.mozilla.android.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.setup.jpake.Gx4IsOneException;
import org.mozilla.gecko.sync.setup.jpake.IncorrectZkpException;
import org.mozilla.gecko.sync.setup.jpake.JPakeClient;
import org.mozilla.gecko.sync.setup.jpake.JPakeCrypto;
import org.mozilla.gecko.sync.setup.jpake.JPakeNumGenerator;
import org.mozilla.gecko.sync.setup.jpake.JPakeNumGeneratorRandom;
import org.mozilla.gecko.sync.setup.jpake.JPakeParty;

public class TestJPakeSetup {
  // Note: will throw NullPointerException if aborts. Only use stateless public
  // methods.
  JPakeClient jClientStateless = new JPakeClient(null);

  @Test
  public void testKeyDerivation() throws UnsupportedEncodingException {
    String keyChars = "37d4d560949aefb501d2c342983242e5e35ecde0528becbca7ca2ae3801fcd21df7af1f02eea754312c9513c6ee599848248d6c28bde8d0efeade2ae061a0d0596b7fcc9e65d13295a05b26b5b96d6df0dc511210acb13058e4490100044eaa668c069eda903cb36b24369eb6d4e9a9e24b716864d63a7634d030b41a2e9f6d3e5339c136f12d0581c422ffa52c4ac685efc90f699a2a7b4bbb0f2c79dbcfe68fc4af715fb0bde8d58570a69f57e15121bfe36907fd6d46b41e7c772ef93c2579f9caf13c9fe4cbbd8a776d1f1c76d9c5aa9aa6ed5e591e5509c9c19f59becd777ad289b41fc018cb0b4a403eda41e614207f97a0f35187c4c343ef752e4c4b113c34b8f5ef0b5c3b42eae9bf461f86f82c0cdc5d7a830600d6347235b0f536c7a4b6d7439633af669263bb571a61b3b5f6686c30d7c967b45a2d45278d4e7eb3d4375bdef8add6bf41a9f299eb4d50f501d4d1a750f5a8640dab3ef99a9efa073743112e040105751d10edf5a1815e24c04c51b1e7f0d6f5560a2ea1c05b095";
    String expectedCrypto64 = "pyNHjnsNGEf7EdpmahMW+e61aI6FlWZi+A/yVTyllE4=";
    String expectedHmac64 = "cUgOr4cusq+bpfK+JCDxKD6w4OjdBNnqflyv7oXhjR8=";

    byte[] cryptoBytes = new byte[32];
    byte[] hmacBytes = new byte[32];
    JPakeCrypto.generateKeyAndHmac(keyChars.getBytes("UTF-8"), cryptoBytes,
        hmacBytes);
    System.out.println("crypto64: "
        + new String(Base64.encodeBase64(cryptoBytes)));
    System.out.println("hmac64: " + new String(Base64.encodeBase64(hmacBytes)));

    assertTrue(expectedCrypto64.equals(Base64.encodeBase64(cryptoBytes)));
    assertTrue(expectedHmac64.equals(Base64.encodeBase64(hmacBytes)));
  }

  @Test
  public void testJPakeCorrectSecret() throws Gx4IsOneException, IncorrectZkpException, IOException, ParseException, NonObjectJSONException, CryptoException {
    String secret = "byubd7u75qmq";
    JPakeNumGenerator gen = new JPakeNumGeneratorRandom();
    assertTrue(jPakeCryptoKeyExchange(gen, gen, secret, secret));
  }

  /*
   * Implementation of a JPake key exchange between two parties with the same
   * server channel; otherwise, JPake fails immediately without communication.
   * Specifically targets JPakeCrypto functionality.
   */
  public boolean jPakeCryptoKeyExchange(JPakeNumGenerator gen1,
      JPakeNumGenerator gen2, String secret1, String secret2)
      throws Gx4IsOneException, IncorrectZkpException, IOException,
      ParseException, NonObjectJSONException, CryptoException {

    // Communicating parties.
    JPakeParty party1 = new JPakeParty("party1");
    JPakeParty party2 = new JPakeParty("party2");

    JPakeCrypto.round1(party1, gen1);
    // After party1 round 1, these values should no longer be null.
    assertNotNull(party1.signerId);
    assertNotNull(party1.x2);
    assertNotNull(party1.gx1);
    assertNotNull(party1.gx2);
    assertNotNull(party1.zkp1);
    assertNotNull(party1.zkp2);
    assertNotNull(party1.zkp1.b);
    assertNotNull(party1.zkp1.gr);
    assertNotNull(party1.zkp1.id);
    assertNotNull(party1.zkp2.b);
    assertNotNull(party1.zkp2.gr);
    assertNotNull(party1.zkp2.id);

    // party2 receives the following values from party1.
    party2.gx3 = party1.gx1;
    party2.gx4 = party1.gx2;
    party2.zkp3 = party1.zkp1;
    party2.zkp4 = party1.zkp2;
    // TODO Run JPakeClient checks.

    JPakeCrypto.round1(party2, gen2);
    // After party2 round 1, these values should no longer be null.
    assertNotNull(party2.signerId);
    assertNotNull(party2.x2);
    assertNotNull(party2.gx1);
    assertNotNull(party2.gx2);
    assertNotNull(party2.zkp1);
    assertNotNull(party2.zkp2);
    assertNotNull(party2.zkp1.b);
    assertNotNull(party2.zkp1.gr);
    assertNotNull(party2.zkp1.id);
    assertNotNull(party2.zkp2.b);
    assertNotNull(party2.zkp2.gr);
    assertNotNull(party2.zkp2.id);

    // Pass relevant values to party1.
    party1.gx3 = party2.gx1;
    party1.gx4 = party2.gx2;
    party1.zkp3 = party2.zkp1;
    party1.zkp4 = party2.zkp2;
    // TODO Run JPakeClient checks.

    JPakeCrypto.round2(secret1, party1, gen1);
    // After party1 round 2, these values should no longer be null.
    assertNotNull(party1.thisA);
    assertNotNull(party1.thisZkpA);
    assertNotNull(party1.thisZkpA.b);
    assertNotNull(party1.thisZkpA.gr);
    assertNotNull(party1.thisZkpA.id);

    // Pass relevant values to party2.
    party2.otherA = party1.thisA;
    party2.otherZkpA = party1.thisZkpA;

    JPakeCrypto.round2(secret2, party2, gen2);
    // Check for nulls.
    assertNotNull(party2.thisA);
    assertNotNull(party2.thisZkpA);
    assertNotNull(party2.thisZkpA.b);
    assertNotNull(party2.thisZkpA.gr);
    assertNotNull(party2.thisZkpA.id);

    // Pass values to party1.
    party1.otherA = party2.thisA;
    party1.otherZkpA = party2.thisZkpA;

    System.out.println(">>> finalRound1");
    KeyBundle keyBundle1 = JPakeCrypto.finalRound(secret1, party1);
    assertNotNull(keyBundle1);
    System.out.println(">>> AES: " + new String(Base64.encodeBase64(keyBundle1.getEncryptionKey())));
    System.out.println(">>> HMAC: " + new String(Base64.encodeBase64(keyBundle1.getHMACKey())));

    // party1 computes the shared key, generates en encrypted message to party2.
    System.out.println("generating verification message");
    ExtendedJSONObject verificationMsg = jClientStateless
        .computeKeyVerification(keyBundle1);
    ExtendedJSONObject payload = verificationMsg.getObject(Constants.JSON_KEY_PAYLOAD);
    String ciphertext1 = (String) payload.get(Constants.JSON_KEY_CIPHERTEXT);
    String iv1 = (String) payload.get(Constants.JSON_KEY_IV);

    // party2 computes the key as well, using its copy of the secret.
    KeyBundle keyBundle2 = JPakeCrypto.finalRound(secret2, party2);
    // party2 fetches the encrypted message and verifies the pairing against its
    // own derived key.
    System.out.println(">>> AES: " + new String(Base64.encodeBase64(keyBundle2.getEncryptionKey())));
    System.out.println(">>> HMAC: " + new String(Base64.encodeBase64(keyBundle2.getHMACKey())));

    boolean isSuccess = jClientStateless.verifyCiphertext(ciphertext1, iv1, keyBundle2);
    return isSuccess;

  }
}
