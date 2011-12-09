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

package org.mozilla.android.sync.setup.jpake;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.mozilla.android.sync.ExtendedJSONObject;
import org.mozilla.android.sync.crypto.Cryptographer;
import org.mozilla.android.sync.crypto.KeyBundle;
import org.mozilla.android.sync.setup.Constants;

public class JpakeCrypto {

  /*
   * primes p and q, and generator g - from original Mozilla jpake
   * implementation
   */
  private static BigInteger p = new BigInteger(
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

  private static BigInteger q = new BigInteger(
      "CFA0478A54717B08CE64805B76E5B14249A77A4838469DF7F7DC987EFCCFB11D", 16);

  private static BigInteger g = new BigInteger(
      "5E5CBA992E0A680D885EB903AEA78E4A45A469103D448EDE3B7ACCC54D521E37F84A4"
          + "BDD5B06B0970CC2D2BBB715F7B82846F9A0C393914C792E6A923E2117AB805276A975"
          + "AADB5261D91673EA9AAFFEECBFA6183DFCB5D3B7332AA19275AFA1F8EC0B60FB6F66C"
          + "C23AE4870791D5982AAD1AA9485FD8F4A60126FEB2CF05DB8A7F0F09B3397F3937F2E"
          + "90B9E5B9C9B6EFEF642BC48351C46FB171B9BFA9EF17A961CE96C7E7A7CC3D3D03DFA"
          + "D1078BA21DA425198F07D2481622BCE45969D9C4D6063D72AB7A0F08B2F49A7CC6AF3"
          + "35E08C4720E31476B67299E231F8BD90B39AC3AE3BE0C6B6CACEF8289A2E2873D58E5"
          + "1E029CAFBD55E6841489AB66B5B4B9BA6E2F784660896AFF387D92844CCB8B6947549"
          + "6DE19DA2E58259B090489AC8E62363CDF82CFD8EF2A427ABCD65750B506F56DDE3B98"
          + "8567A88126B914D7828E2B63A6D7ED0747EC59E0E0A23CE7D8A74C1D2C2A7AFB6A297"
          + "99620F00E11C33787F7DED3B30E1A22D09F1FBDA1ABBBFBF25CAE05A13F812E34563F"
          + "99410E73B", 16);

  public static void round1(String mySignerId, ExtendedJSONObject values) {
    // randomly select x1 from [0,q], x2 from [1,q]
    BigInteger x1 = getRandom(q); // [0, q)
    BigInteger x2 = BigInteger.ONE.add(getRandom(q.subtract(BigInteger.ONE))); // [1,
                                                                               // q)

    BigInteger gx1 = g.modPow(x1, p);
    BigInteger gx2 = g.modPow(x2, p);

    // generate zero knowledge proofs
    String[] gZkp1 = getZkp(p, q, g, x1, mySignerId);
    String[] gZkp2 = getZkp(p, q, g, x2, mySignerId);

    values.put(Constants.GX1, gx1.toString(16));
    values.put(Constants.GX2, gx2.toString(16));
    values.put(Constants.ZKP_X1, gZkp1[0]);
    values.put(Constants.ZKP_X2, gZkp2[0]);
    values.put(Constants.R1, gZkp1[1]);
    values.put(Constants.R2, gZkp2[1]);
  }

  public static void round2(String theirSignerId, ExtendedJSONObject valuesOut,
      String secret, BigInteger gx1, BigInteger gx2, BigInteger gx3,
      BigInteger gx4, BigInteger r3, BigInteger r4, BigInteger gr3,
      BigInteger gr4) throws Gx4IsOneException {
    if (gx4 == BigInteger.ONE) {
      throw new Gx4IsOneException();
    }

    // check ZKP
    checkZkp(g, gx3, gr3);
    checkZkp(g, gx4, gr4);

    // compute a = g^[(x1+x3+x4)*x2*secret)]
    BigInteger y1 = gx3.multiply(gr4).mod(p).multiply(gx1).mod(p);
    BigInteger y2 = gx2.multiply(new BigInteger(secret.getBytes())).mod(p);

    BigInteger a = y1.modPow(y2, p);
    String[] aZkp = getZkp(p, q, g, a, theirSignerId);

    valuesOut.put(Constants.A, a);
    valuesOut.put(Constants.ZKP_A, aZkp[0]);
    valuesOut.put(Constants.R1, aZkp[1]);
  }

  public static String finalRound(BigInteger b, BigInteger zkp_B, BigInteger gx1,
      BigInteger gx2, BigInteger gx3, BigInteger gx4, String s, BigInteger x2) {

    BigInteger g1 = gx1.multiply(gx2).mod(p).multiply(gx3).mod(p);
    checkZkp(g1, b, zkp_B);

    // Calculate shared key g^(x1+x3)x2*x4*s, which is equivalent to
    // (B/g^(x2*x4*s))^x2 = (B*(g^x4)^x2^s^-1)^2
    BigInteger y1 = gx4.modPow(x2, p); // gx4^x2
    BigInteger minusS = q.subtract(new BigInteger(s.getBytes())).mod(p);
    y1 = y1.modPow(minusS, p); // gx4^x2^-s
    y1 = b.multiply(y1).mod(p); // B*(gx4^x2^-s)
    y1 = y1.modPow(x2, p);

    String key = null;
    try {
      key = Cryptographer.sha1Base32(y1.toString(16));
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return key;

  }
  /* Helper Methods */

  private static String[] getZkp(BigInteger p, BigInteger q, BigInteger g,
      BigInteger x, String id) {
    String[] result = new String[2];

    // generate random r for exponent
    BigInteger r = getRandom(q);

    BigInteger gr = g.modPow(r, p);
    result[0] = gr.toString(16);

    // TODO: compute hash(g, gr, gx, id)
    result[1] = "v-x*h";
    return result;
  }

  private static void checkZkp(BigInteger g1, BigInteger gx, BigInteger gr) {
    // TODO: check zkp
  }

  /*
   * Helper Function to generate a uniformly random value in [0, q]
   */
  private static BigInteger getRandom(BigInteger q) {
    int maxBytes = (int) Math.ceil(q.bitLength() / 8);

    byte[] bytes = new byte[maxBytes];
    new SecureRandom().nextBytes(bytes);
    BigInteger randInt = new BigInteger(bytes);
    // TODO: is this going to be very slow?
    // TODO: add some bit shifting/masking to decrease mod computation
    return randInt.mod(q);
  }
}