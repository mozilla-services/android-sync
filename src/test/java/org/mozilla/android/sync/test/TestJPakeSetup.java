package org.mozilla.android.sync.test;

import static org.junit.Assert.assertArrayEquals;

import java.io.UnsupportedEncodingException;

import org.junit.Test;
import org.mozilla.gecko.sync.setup.jpake.JPakeCrypto;
import org.mozilla.gecko.sync.setup.jpake.JPakeNumGenerator;
import org.mozilla.gecko.sync.setup.jpake.JPakeParty;

public class TestJPakeSetup {

  @Test
  public void testKeyDerivation() {
    String keyChars = "0123456789abcdef0123456789abcdef";
    String expectedCrypto = "";
    String expectedHmac = "";


    byte[] expectedCryptoBytes = null;
    byte[] expectedHmacBytes = null;
    try {
      expectedCryptoBytes = expectedCrypto.getBytes("UTF-8");
      expectedHmacBytes = expectedHmac.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    JPakeCrypto jp = new JPakeCrypto();
    byte[] cryptoBytes = new byte[32];
    byte[] hmacBytes = new byte[32];
    try {
      jp.generateKeyAndHmac(keyChars.getBytes("UTF-8"), cryptoBytes, hmacBytes);
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    assertArrayEquals(expectedCryptoBytes, cryptoBytes);
    assertArrayEquals(expectedHmacBytes, hmacBytes);
  }

  /*
   * Implementation of a JPake key exchange between two parties.
   * party1 always intiates.
   */
  public void jPakeExchange(JPakeNumGenerator gen1, JPakeNumGenerator gen2, String secret) {
    JPakeCrypto jc = new JPakeCrypto();

    JPakeParty party1 = new JPakeParty("party1");
    JPakeParty party2 = new JPakeParty("party2");
  }
}
