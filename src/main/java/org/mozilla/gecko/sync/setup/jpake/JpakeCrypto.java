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

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Mac;

import org.mozilla.android.sync.crypto.HKDF;
import org.mozilla.android.sync.crypto.KeyBundle;
import org.mozilla.android.sync.crypto.Utils;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.setup.Constants;

public class JpakeCrypto {

  /*
   * primes p and q, and generator g - from original Mozilla jpake
   * implementation
   */
  private static final BigInteger P                = new BigInteger(
                                                       "90066455B5CFC38F9CAA4A48B4281F292C260FEEF01FD61037E56258A7795A1C7AD46"
                                                           + "076982CE6BB956936C6AB4DCFE05E6784586940CA544B9B2140E1EB523F009D20A7E7"
                                                           + "880E4E5BFA690F1B9004A27811CD9904AF70420EEFD6EA11EF7DA129F58835FF56B89"
                                                           + "FAA637BC9AC2EFAAB903402229F491D8D3485261CD068699B6BA58A1DDBBEF6DB51E8"
                                                           + "FE34E8A78E542D7BA351C21EA8D8F1D29F5D5D15939487E27F4416B0CA632C59EFD1B"
                                                           + "1EB66511A5A0FBF615B766C5862D0BD8A3FE7A0E0DA0FB2FE1FCB19E8F9996A8EA0FC"
                                                           + "CDE538175238FC8B0EE6F29AF7F642773EBE8CD5402415A01451A840476B2FCEB0E38"
                                                           + "8D30D4B376C37FE401C2A2C2F941DAD179C540C1C8CE030D460C4D983BE9AB0B20F69"
                                                           + "144C1AE13F9383EA1C08504FB0BF321503EFE43488310DD8DC77EC5B8349B8BFE97C2"
                                                           + "C560EA878DE87C11E3D597F1FEA742D73EEC7F37BE43949EF1A0D15C3F3E3FC0A8335"
                                                           + "617055AC91328EC22B50FC15B941D3D1624CD88BC25F3E941FDDC6200689581BFEC41"
                                                           + "6B4B2CB73", 16);

  private static final BigInteger Q                = new BigInteger(
                                                       "CFA0478A54717B08CE64805B76E5B14249A77A4838469DF7F7DC987EFCCFB11D",
                                                       16);

  private static final BigInteger G                = new BigInteger(
                                                       "5E5CBA992E0A680D885EB903AEA78E4A45A469103D448EDE3B7ACCC54D521E37F84A4"
                                                           + "BDD5B06B0970CC2D2BBB715F7B82846F9A0C393914C792E6A923E2117AB805276A975"
                                                           + "AADB5261D91673EA9AAFFEECBFA6183DFCB5D3B7332AA19275AFA1F8EC0B60FB6F66C"
                                                           + "C23AE4870791D5982AAD1AA9485FD8F4A60126FEB2CF05DB8A7F0F09B3397F3937F2E"
                                                           + "90B9E5B9C9B6EFEF642BC48351C46FB171B9BFA9EF17A961CE96C7E7A7CC3D3D03DFA"
                                                           + "D1078BA21DA425198F07D2481622BCE45969D9C4D6063D72AB7A0F08B2F49A7CC6AF3"
                                                           + "35E08C4720E31476B67299E231F8BD90B39AC3AE3BE0C6B6CACEF8289A2E2873D58E5"
                                                           + "1029CAFBD55E6841489AB66B5B4B9BA6E2F784660896AFF387D92844CCB8B6947549"
                                                           + "6DE19DA2E58259B090489AC8E62363CDF82CFD8EF2A427ABCD65750B506F56DDE3B98"
                                                           + "8567A88126B914D7828E2B63A6D7ED0747EC59E0E0A23CE7D8A74C1D2C2A7AFB6A297"
                                                           + "99620F00E11C33787F7DED3B30E1A22D09F1FBDA1ABBBFBF25CAE05A13F812E34563F"
                                                           + "99410E73B", 16);

  // HKDF params for generating encryption key and HMAC.
  private static final byte[]     EMPTY_BYTES      = {};
  private static final byte[]     ENCR_INPUT_BYTES = { 1 };
  private static final byte[]     HMAC_INPUT_BYTES = { 2 };

  // Jpake params for this key exchange.
  private BigInteger              x2;
  private BigInteger              gx1;
  private BigInteger              gx2;
  private BigInteger              gx3;
  private BigInteger              gx4;

  public void round1(String mySignerId, ExtendedJSONObject values) {
    // randomly select x1 from [0,q], x2 from [1,q]
    BigInteger x1 = getRandom(Q); // [0, q)
    // [1, q)
    BigInteger x2 = this.x2 = BigInteger.ONE.add(getRandom(Q
        .subtract(BigInteger.ONE)));

    BigInteger gx1 = this.gx1 = G.modPow(x1, P);
    BigInteger gx2 = this.gx2 = G.modPow(x2, P);

    // generate zero knowledge proofs
    String[] gZkp1 = getZkp(P, Q, G, x1, mySignerId);
    String[] gZkp2 = getZkp(P, Q, G, x2, mySignerId);

    values.put(Constants.GX1, gx1.toString(16));
    values.put(Constants.GX2, gx2.toString(16));
    values.put(Constants.ZKP_X1, gZkp1[0]);
    values.put(Constants.ZKP_X2, gZkp2[0]);
    values.put(Constants.B1, gZkp1[1]);
    values.put(Constants.B2, gZkp2[1]);
  }

  public void round2(String theirSignerId, ExtendedJSONObject valuesOut,
      String secret, BigInteger gx3, BigInteger gx4, ExtendedJSONObject zkp1,
      ExtendedJSONObject zkp2) throws Gx4IsOneException, IncorrectZkpException {

    if (gx4 == BigInteger.ONE) {
      throw new Gx4IsOneException();
    }

    // check ZKP
    checkZkp(G, gx3, zkp1);
    checkZkp(G, gx4, zkp2);

    this.gx3 = gx3;
    this.gx4 = gx4;

    // compute a = g^[(x1+x3+x4)*x2*secret)]
    BigInteger y1 = gx3.multiply(gx4).mod(P).multiply(gx1).mod(P);
    BigInteger y2 = this.x2.multiply(new BigInteger(secret.getBytes())).mod(P);

    BigInteger a = y1.modPow(y2, P);
    String[] aZkp = getZkp(P, Q, G, a, theirSignerId);

    valuesOut.put(Constants.A, a);
    valuesOut.put(Constants.ZKP_A, aZkp[0]);
    valuesOut.put(Constants.B, aZkp[1]);
  }

  public KeyBundle finalRound(BigInteger b, ExtendedJSONObject zkp_B, String s) throws IncorrectZkpException {

    BigInteger g1 = this.gx1.multiply(this.gx2).mod(P).multiply(this.gx3)
        .mod(P);
    checkZkp(g1, b, zkp_B);

    // Calculate shared key g^(x1+x3)x2*x4*s, which is equivalent to
    // (B/g^(x2*x4*s))^x2 = (B*(g^x4)^x2^s^-1)^2
    BigInteger y1 = this.gx4.modPow(this.x2, P); // gx4^x2
    BigInteger minusS = Q.subtract(new BigInteger(s.getBytes())).mod(P);
    y1 = y1.modPow(minusS, P); // gx4^x2^-s
    y1 = b.multiply(y1).mod(P); // B*(gx4^x2^-s)
    y1 = y1.modPow(this.x2, P);

    // Generate HMAC and Encryption keys from synckey.
    byte[] synckey = y1.toByteArray();
    Mac hmacHasher = HKDF.makeHMACHasher(synckey);
    // TODO: ok w/o username?
    byte[] encrBytes = Utils.concatAll(EMPTY_BYTES, HKDF.HMAC_INPUT,
        ENCR_INPUT_BYTES);
    byte[] encrKey = HKDF.digestBytes(encrBytes, hmacHasher);
    byte[] hmacBytes = Utils.concatAll(encrKey, HKDF.HMAC_INPUT,
        HMAC_INPUT_BYTES);

    return new KeyBundle(encrKey, hmacBytes);
  }

  /* Helper Methods */

  private String[] getZkp(BigInteger p, BigInteger q, BigInteger g,
      BigInteger x, String id) {
    String[] result = new String[2];

    // generate random r for exponent
    BigInteger r = getRandom(q);

    BigInteger gr = g.modPow(r, p);
    result[0] = gr.toString(16);
    BigInteger gx = g.modPow(x, p);

    BigInteger b = computeBHash(g, gr, gx, id);
    // ZKP value: r-x*h
    result[1] = r.subtract(x.multiply(b)).mod(p).toString(16);

    return result;
  }

  private void checkZkp(BigInteger g1, BigInteger gx, ExtendedJSONObject zkp)
      throws IncorrectZkpException {
    // extract zkp
    BigInteger gr = new BigInteger((String) zkp.get(Constants.GR), 16);
    BigInteger s = new BigInteger((String) zkp.get(Constants.B), 16);
    String signerId = (String) zkp.get(Constants.ID);

    BigInteger h = computeBHash(g1, gr, gx, signerId);
    // Check parameters of zkp, and compare to computed hash
    if ((gx.compareTo(BigInteger.ZERO) < 1) // g^x > 1
        || gx.compareTo(P.subtract(BigInteger.ONE)) > -1 // g^x < p-1
        || gx.modPow(Q, P).compareTo(BigInteger.ONE) == 0 // g^x^q = 1
        /*
         * Since s = r+h*x
         * g^s = g^r*g^x^h
         */
        || g1.modPow(s, P).compareTo(gr.multiply(gx.modPow(h, P))) != 0) {
      throw new IncorrectZkpException();
    }
  }

  private BigInteger computeBHash(BigInteger g, BigInteger gr, BigInteger gx,
      String id) {
    MessageDigest sha = null;
    try {
      sha = MessageDigest.getInstance("SHA-1");

      sha.update(g.toByteArray());
      sha.update(gr.toByteArray());
      sha.update(gx.toByteArray());
      sha.update(id.getBytes());
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return new BigInteger(sha.digest());
  }

  /*
   * Helper Function to generate a uniformly random value in [0, q]
   */
  private BigInteger getRandom(BigInteger q) {
    int maxBytes = (int) Math.ceil(q.bitLength() / 8);

    byte[] bytes = new byte[maxBytes];
    new SecureRandom().nextBytes(bytes);
    BigInteger randInt = new BigInteger(bytes);
    // TODO: is this going to be very slow?
    // TODO: add some bit shifting/masking to decrease mod computation
    return randInt.mod(q);
  }
}