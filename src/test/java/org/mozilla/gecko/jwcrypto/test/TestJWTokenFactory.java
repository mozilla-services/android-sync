package org.mozilla.gecko.jwcrypto.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.gecko.jwcrypto.JWCryptoImplementation;
import org.mozilla.gecko.jwcrypto.JWTokenFactory;
import org.mozilla.gecko.jwcrypto.RSACryptoImplementation;
import org.mozilla.gecko.sync.ExtendedJSONObject;

public class TestJWTokenFactory {
  protected JWCryptoImplementation cryptoImplementation;

  @Before
  public void setUp() {
    cryptoImplementation = new RSACryptoImplementation();
  }

  @Test
  public void testEncodeDecodeSuccess() throws Exception {
    JWTokenFactory tokenFactory = new JWTokenFactory("RS256", cryptoImplementation);

    KeyPair keyPair = cryptoImplementation.generateKeypair(1024);
    PrivateKey privateKey = keyPair.getPrivate();
    PublicKey publicKey = keyPair.getPublic();

    ExtendedJSONObject o = new ExtendedJSONObject();
    o.put("key", "value");

    String token = tokenFactory.encode(o.toJSONString(), privateKey);
    assertNotNull(token);

    String payload = tokenFactory.decode(token, publicKey);
    assertEquals(o.toJSONString(), payload);

    try {
      tokenFactory.decode(token + "x", publicKey);
      fail("Expected exception.");
    } catch (GeneralSecurityException e) {
      // Do nothing.
    }
  }
}
