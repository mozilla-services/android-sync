/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Android Sync Client.
 *
 * The Initial Developer of the Original Code is
 * the Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *  Chenxia Liu <liuche@mozilla.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.gecko.sync.setup.jpake;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.mozilla.android.sync.crypto.HKDF;
import org.mozilla.android.sync.crypto.KeyBundle;
import org.mozilla.android.sync.crypto.Utils;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.setup.Constants;

import android.util.Log;

public class JpakeCrypto {
  private static final String     LOG_TAG              = "JpakeCrypto";

  /*
   * Primes P and Q, and generator G - from original Mozilla jpake
   * implementation.
   */
  private static final BigInteger P                = new BigInteger(
                                                       "90066455B5CFC38F9CAA4A48B4281F292C260FEEF01FD61037E56258A7795A1C"
                                                           + "7AD46076982CE6BB956936C6AB4DCFE05E6784586940CA544B9B2140E1EB523F"
                                                           + "009D20A7E7880E4E5BFA690F1B9004A27811CD9904AF70420EEFD6EA11EF7DA1"
                                                           + "29F58835FF56B89FAA637BC9AC2EFAAB903402229F491D8D3485261CD068699B"
                                                           + "6BA58A1DDBBEF6DB51E8FE34E8A78E542D7BA351C21EA8D8F1D29F5D5D159394"
                                                           + "87E27F4416B0CA632C59EFD1B1EB66511A5A0FBF615B766C5862D0BD8A3FE7A0"
                                                           + "E0DA0FB2FE1FCB19E8F9996A8EA0FCCDE538175238FC8B0EE6F29AF7F642773E"
                                                           + "BE8CD5402415A01451A840476B2FCEB0E388D30D4B376C37FE401C2A2C2F941D"
                                                           + "AD179C540C1C8CE030D460C4D983BE9AB0B20F69144C1AE13F9383EA1C08504F"
                                                           + "B0BF321503EFE43488310DD8DC77EC5B8349B8BFE97C2C560EA878DE87C11E3D"
                                                           + "597F1FEA742D73EEC7F37BE43949EF1A0D15C3F3E3FC0A8335617055AC91328E"
                                                           + "C22B50FC15B941D3D1624CD88BC25F3E941FDDC6200689581BFEC416B4B2CB73",
                                                       16);

  private static final BigInteger Q                = new BigInteger(
                                                       "CFA0478A54717B08CE64805B76E5B14249A77A4838469DF7F7DC987EFCCFB11D",
                                                       16);

  private static final BigInteger G                = new BigInteger(
                                                             "5E5CBA992E0A680D885EB903AEA78E4A45A469103D448EDE3B7ACCC54D521E37"
                                                           + "F84A4BDD5B06B0970CC2D2BBB715F7B82846F9A0C393914C792E6A923E2117AB"
                                                           + "805276A975AADB5261D91673EA9AAFFEECBFA6183DFCB5D3B7332AA19275AFA1"
                                                           + "F8EC0B60FB6F66CC23AE4870791D5982AAD1AA9485FD8F4A60126FEB2CF05DB8"
                                                           + "A7F0F09B3397F3937F2E90B9E5B9C9B6EFEF642BC48351C46FB171B9BFA9EF17"
                                                           + "A961CE96C7E7A7CC3D3D03DFAD1078BA21DA425198F07D2481622BCE45969D9C"
                                                           + "4D6063D72AB7A0F08B2F49A7CC6AF335E08C4720E31476B67299E231F8BD90B3"
                                                           + "9AC3AE3BE0C6B6CACEF8289A2E2873D58E51E029CAFBD55E6841489AB66B5B4B"
                                                           + "9BA6E2F784660896AFF387D92844CCB8B69475496DE19DA2E58259B090489AC8"
                                                           + "E62363CDF82CFD8EF2A427ABCD65750B506F56DDE3B988567A88126B914D7828"
                                                           + "E2B63A6D7ED0747EC59E0E0A23CE7D8A74C1D2C2A7AFB6A29799620F00E11C33"
                                                           + "787F7DED3B30E1A22D09F1FBDA1ABBBFBF25CAE05A13F812E34563F99410E73B",
                                                       16);

  // HKDF params for generating encryption key and HMAC.
  private static final byte[]     ENCR_INPUT_BYTES = { 1 };

  // Jpake params for this key exchange.
  private String                  mySignerId;
  private BigInteger              x2;
  private BigInteger              gx1;
  private BigInteger              gx2;
  private BigInteger              gx3;
  private BigInteger              gx4;

  /**
<<<<<<< HEAD
=======
   *
   * Round 1 of JPAKE protocol.
   * Generate x1, x2, and ZKP for them.
>>>>>>> JPake implemented, error on last step.
   *
   * @param mySignerId
   * @param valuesOut
   */
  public void round1(String mySignerId, ExtendedJSONObject valuesOut) {
    // mySignerId used for creating ZKP.
    this.mySignerId = mySignerId;

    // Randomly select x1 from [0,q), x2 from [1,q).
    BigInteger x1 = getRandom(Q); // [0, q)
    BigInteger x2 = this.x2 = BigInteger.ONE.add(getRandom(Q
        .subtract(BigInteger.ONE))); // [1, q)

    BigInteger gx1 = G.modPow(x1, P);
    this.gx1 = gx1;
    BigInteger gx2 = G.modPow(x2, P);
    this.gx2 = gx2;

    // Generate zero knowledge proofs.
    String[] zkp1 = createZkp(G, x1, gx1);
    String[] zkp2 = createZkp(G, x2, gx2);

    // Store round1 return values.
    valuesOut.put(Constants.ZKP_KEY_GX1, BigIntegerHelper.toEvenLengthHex(gx1));
    valuesOut.put(Constants.ZKP_KEY_GX2, BigIntegerHelper.toEvenLengthHex(gx2));
    valuesOut.put(Constants.CRYPTO_KEY_GR1, zkp1[0]);
    valuesOut.put(Constants.CRYPTO_KEY_GR2, zkp2[0]);
    valuesOut.put(Constants.ZKP_KEY_B1, zkp1[1]);
    valuesOut.put(Constants.ZKP_KEY_B2, zkp2[1]);
  }

  /**
<<<<<<< HEAD
=======
   * Round 2 of JPAKE protocol.
   * Generate A and ZKP for A.
   * Verify ZKP from other party. Does not check for replay ZKP.
>>>>>>> JPake implemented, error on last step.
   *
   * @param mySignerId
   * @param valuesOut
   * @param secret
   * @param gx3
   * @param gx4
   * @param zkp3
   * @param zkp4
   * @throws Gx4IsOneException
   * @throws IncorrectZkpException
   */
  public void round2(String mySignerId, ExtendedJSONObject valuesOut,
      String secret, BigInteger gx3, BigInteger gx4, ExtendedJSONObject zkp3,
      ExtendedJSONObject zkp4) throws Gx4IsOneException, IncorrectZkpException {

    Log.d(LOG_TAG, "round2 started");

    if (gx4 == BigInteger.ONE) {
      throw new Gx4IsOneException();
    }

    // Check ZKP.
    checkZkp(G, gx3, zkp3);
    checkZkp(G, gx4, zkp4);

    this.gx3 = gx3;
    this.gx4 = gx4;

    // Compute a = g^[(x1+x3+x4)*(x2*secret)].
    BigInteger y1 = gx3.multiply(gx4).mod(P).multiply(gx1).mod(P);
    BigInteger y2 = null;
    try {
      y2 = this.x2.multiply(new BigInteger(secret.getBytes("ascii"))).mod(P);
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    BigInteger a = y1.modPow(y2, P);
    String[] zkpA = createZkp(y1, y2, a);

    valuesOut.put(Constants.ZKP_KEY_A, BigIntegerHelper.toEvenLengthHex(a));
    valuesOut.put(Constants.ZKP_KEY_ZKP_A, zkpA[0]);
    valuesOut.put(Constants.ZKP_KEY_B, zkpA[1]);
    Log.d(LOG_TAG, "round2 finished");
  }

  /**
   * Final round of JPAKE protocol.
   *
   * @param b
   * @param zkp
   * @param secret
   *
   * @return KeyBundle
   * @throws IncorrectZkpException
   */
  public KeyBundle finalRound(BigInteger b, ExtendedJSONObject zkp, String secret)
      throws IncorrectZkpException {
    Log.d(LOG_TAG, "final round started");

    BigInteger g123 = this.gx1.multiply(this.gx2).mod(P).multiply(this.gx3)
        .mod(P);
    checkZkp(g123, b, zkp);

    // Calculate shared key g^(x1+x3)*x2*x4*secret, which is equivalent to
    // (B/g^(x2*x4*s))^x2 = (B*(g^x4)^x2^s^-1)^2.

    BigInteger k = this.gx4.modPow(this.x2, P); // gx4^x2
    BigInteger negS = Q.subtract(new BigInteger(secret.getBytes())).mod(P);
    k = k.modPow(negS.negate(), P); // gx4^x2^-s
    k = b.multiply(k).mod(P); // B*(gx4^x2^-s)
    k = k.modPow(this.x2, P); // to power x2

    // Generate HMAC and Encryption keys from synckey.
    byte[] prk = k.toByteArray();

    // TODO: make sure is correct format
    byte[] okm = HKDF.hkdfExpand(prk, HKDF.HMAC_INPUT, 32 * 2);
    byte[] enc = new byte[32];
    byte[] hmac = new byte[32];
    System.arraycopy(okm, 0, enc, 0, 32);
    System.arraycopy(okm, 32, hmac, 0, 32);

    Log.d(LOG_TAG, "final round finished; returning key");
    return new KeyBundle(enc, hmac);
  }

  /* Helper Methods */

  /*
   * Generate the ZKP b = r - x*h, and g^r, where h = hash(g, g^r, g^x, id). (We
   * pass in gx to save on an exponentiation of g^x)
   */
  private String[] createZkp(BigInteger g, BigInteger x, BigInteger gx) {
    String[] result = new String[2];

    // Generate random r for exponent.
    BigInteger r = getRandom(Q);

    // Calculate g^r for ZKP.
    BigInteger gr = g.modPow(r, P);
    result[0] = BigIntegerHelper.toEvenLengthHex(gr);

    // Calculate the ZKP b value = (r-x*h) % q.
    BigInteger h = computeBHash(g, gr, gx, mySignerId);
    Log.e(LOG_TAG, "myhash: " + h.toString(16));
    // ZKP value = b = r-x*h
    BigInteger b = r.subtract(x.multiply(h)).mod(Q);
    result[1] = BigIntegerHelper.toEvenLengthHex(b);

    return result;
  }

  /*
   * Verify ZKP.
   */
  private void checkZkp(BigInteger g, BigInteger gx, ExtendedJSONObject zkp)
      throws IncorrectZkpException {
    // Extract ZKP params.
    BigInteger gr = new BigInteger((String) zkp.get(Constants.ZKP_KEY_GR), 16);
    BigInteger b = new BigInteger((String) zkp.get(Constants.ZKP_KEY_B), 16);
    String signerId = (String) zkp.get(Constants.ZKP_KEY_ID);

    BigInteger h = computeBHash(g, gr, gx, signerId);

    // Check parameters of zkp, and compare to computed hash. These shouldn't
    // fail.
    if (gx.compareTo(BigInteger.ZERO) < 1) {// g^x > 1
      Log.e(LOG_TAG, "g^x > 1 fails");
      throw new IncorrectZkpException();
    } else if (gx.compareTo(P.subtract(BigInteger.ONE)) > -1) { // g^x < p-1
      Log.e(LOG_TAG, "g^x < p-1 fails");
      throw new IncorrectZkpException();
    } else if (gx.modPow(Q, P).compareTo(BigInteger.ONE) != 0) {
      Log.e(LOG_TAG, "g^x^q % p = 1 fails");
    } else if (gr.compareTo(g.modPow(b, P).multiply(gx.modPow(h, P)).mod(P)) != 0) {
      // b = r-h*x ==> g^r = g^b*g^x^(h)
      Log.i(LOG_TAG, "gb*g(xh) = "
          + g.modPow(b, P).multiply(gx.modPow(h, P)).mod(P).toString(16));
      Log.d(LOG_TAG, "gr = " + gr.toString(16));
      Log.d(LOG_TAG, "b = " + b.toString(16));
      Log.d(LOG_TAG, "g^b = " + g.modPow(b, P).toString(16));
      Log.d(LOG_TAG, "g^(xh) = " + gx.modPow(h, P).toString(16));
      Log.d(LOG_TAG, "gx = " + gx.toString(16));
      Log.d(LOG_TAG, "h = " + h.toString(16));
      Log.e(LOG_TAG, "zkp calculation incorrect");
      throw new IncorrectZkpException();
    }
    else {
      Log.d(LOG_TAG, "*** ZKP SUCCESS ***");
    }
  }

  /*
   * Use SHA-256 to compute a BigInteger hash of g, gr, gx values with
   * mySignerId to prevent replay. Does not make a twos-complement BigInteger
   * form hash.
   */
  private BigInteger computeBHash(BigInteger g, BigInteger gr, BigInteger gx,
      String id) {
    MessageDigest sha = null;
    try {
      sha = MessageDigest.getInstance("SHA-256");
      sha.reset();

      /*
       * Note: you should ensure the items in H(...) have clear boundaries. It
       * is simple if the other party knows sizes of g, gr, gx and signerID and
       * hence the boundary is unambiguous. If not, you'd better prepend each
       * item with its byte length, but I've omitted that here.
       */

      hashByteArrayWithLength(sha,
          BigIntegerHelper.BigIntegerToByteArrayWithoutSign(g));
      hashByteArrayWithLength(sha,
          BigIntegerHelper.BigIntegerToByteArrayWithoutSign(gr));
      hashByteArrayWithLength(sha,
          BigIntegerHelper.BigIntegerToByteArrayWithoutSign(gx));
      hashByteArrayWithLength(sha, id.getBytes());
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    byte[] hash = sha.digest();

    return BigIntegerHelper.ByteArrayToBigIntegerWithoutSign(hash);
  }

  private static void hashByteArrayWithLength(MessageDigest sha, byte[] data) {
    int length = data.length;
    byte[] b = new byte[] { (byte) (length >>> 8), (byte) (length & 0xff) };
    sha.update(b);
    sha.update(data);
  }

  /*
   * Helper Function to generate a uniformly random value in [0, q].
   */
  private BigInteger getRandom(BigInteger q) {
    int maxBytes = (int) Math.ceil(((double) q.bitLength()) / 8);

    byte[] bytes = new byte[maxBytes];
    new SecureRandom().nextBytes(bytes);
    BigInteger randInt = new BigInteger(bytes);
    // TODO: is this going to be very slow?
    // add some bit shifting/masking to decrease mod computation
    return randInt.mod(q);
  }
}