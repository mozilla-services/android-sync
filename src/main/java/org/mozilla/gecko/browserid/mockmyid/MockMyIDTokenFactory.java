/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.browserid.mockmyid;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import org.mozilla.gecko.browserid.crypto.BrowserIDTokenFactory;
import org.mozilla.gecko.jwcrypto.RSACryptoImplementation;

public class MockMyIDTokenFactory {
  public static final String     MOCKMYID_ALGORITHM        = "RS256";
  public static final BigInteger MOCKMYID_MODULUS          = new BigInteger("15498874758090276039465094105837231567265546373975960480941122651107772824121527483107402353899846252489837024870191707394743196399582959425513904762996756672089693541009892030848825079649783086005554442490232900875792851786203948088457942416978976455297428077460890650409549242124655536986141363719589882160081480785048965686285142002320767066674879737238012064156675899512503143225481933864507793118457805792064445502834162315532113963746801770187685650408560424682654937744713813773896962263709692724630650952159596951348264005004375017610441835956073275708740239518011400991972811669493356682993446554779893834303");
  public static final BigInteger MOCKMYID_PUBLIC_EXPONENT  = new BigInteger("65537");
  public static final BigInteger MOCKMYID_PRIVATE_EXPONENT = new BigInteger("6539906961872354450087244036236367269804254381890095841127085551577495913426869112377010004955160417265879626558436936025363204803913318582680951558904318308893730033158178650549970379367915856087364428530828396795995781364659413467784853435450762392157026962694408807947047846891301466649598749901605789115278274397848888140105306063608217776127549926721544215720872305194645129403056801987422794114703255989202755511523434098625000826968430077091984351410839837395828971692109391386427709263149504336916566097901771762648090880994773325283207496645630792248007805177873532441314470502254528486411726581424522838833");

  public static final RSACryptoImplementation rsaCryptoImplementation = new RSACryptoImplementation();
  // Computed lazily by static <code>getMockMyIDPrivateKey</code>.
  protected static PrivateKey cachedMockMyIDPrivateKey;

  public final BrowserIDTokenFactory browserIdTokenFactory;


  public MockMyIDTokenFactory() {
    this.browserIdTokenFactory = new BrowserIDTokenFactory(MOCKMYID_ALGORITHM, rsaCryptoImplementation, rsaCryptoImplementation);
  }

  public static PrivateKey getMockMyIDPrivateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
    if (cachedMockMyIDPrivateKey == null) {
      cachedMockMyIDPrivateKey = rsaCryptoImplementation.createPrivateKey(MOCKMYID_MODULUS, MOCKMYID_PRIVATE_EXPONENT);
    }

    return cachedMockMyIDPrivateKey;
  }

  /**
   * Sign a public key asserting ownership of username@mockmyid.com with
   * mockmyid.com's private key.
   *
   * @param publicKeyToSign
   *          public key to sign.
   * @param username
   *          sign username@mockmyid.com
   * @param issuedAt
   *          timestamp for certificate.
   * @param durationInMilliseconds
   *          lifespan of certificate, in milliseconds.
   * @return Java Web Signature.
   * @throws Exception
   */
  public String createMockMyIDCertificate(final PublicKey publicKeyToSign, final String username,
      final long issuedAt, final long durationInMilliseconds)
      throws Exception {
    final long expiresAt = issuedAt + durationInMilliseconds;

    PrivateKey mockMyIdPrivateKey = getMockMyIDPrivateKey();

    return browserIdTokenFactory.createCertificate(publicKeyToSign, username + "@mockmyid.com", "mockmyid.com", issuedAt, expiresAt, mockMyIdPrivateKey);
  }

  /**
   * Sign a public key asserting ownership of username@mockmyid.com with
   * mockmyid.com's private key.
   *
   * @param publicKeyToSign
   *          public key to sign.
   * @param username
   *          sign username@mockmyid.com
   * @return Java Web Signature.
   * @throws Exception
   */
  public String createMockMyIDCertificate(final PublicKey publicKeyToSign, final String username)
      throws Exception {
    return createMockMyIDCertificate(publicKeyToSign, username,
        System.currentTimeMillis(), 60 * 60 * 1000);
  }

  /**
   * Generate a BrowserID assertion.
   */
  public String createMockMyIDAssertion(String username, String audience,
      long issuedAt, long durationInMilliseconds)
      throws Exception {
    final KeyPair pair = rsaCryptoImplementation.generateKeypair(2048);
    final PublicKey publicKeyToSign = pair.getPublic();
    final PrivateKey privateKeyToSignWith = pair.getPrivate();

    String certificate = createMockMyIDCertificate(publicKeyToSign, username, issuedAt, durationInMilliseconds);

    final String issuer = "127.0.0.1";

    return browserIdTokenFactory.createAssertion(privateKeyToSignWith, certificate, issuer, audience);
  }

  /**
   * Generate a BrowserID assertion.
   */
  public String createMockMyIDAssertion(String username, String audience) throws Exception {
    return createMockMyIDAssertion(username, audience, System.currentTimeMillis(), 60 * 60 * 1000);
  }
}
