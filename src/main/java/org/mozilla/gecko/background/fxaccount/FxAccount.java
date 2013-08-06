/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.fxaccount;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.HKDF;
import org.mozilla.gecko.sync.net.SRPConstants;
import org.mozilla.gecko.sync.net.SRPConstants.Parameters;

public class FxAccount {
  public final String email;
  public final String mainSalt;
  public final String srpSalt;
  public final byte[] srpPWBytes;
  public final byte[] unwrapBKeyBytes;

  public final BigInteger x;
  public final BigInteger v;
  public final Parameters params;

  protected FxAccount(SRPConstants.Parameters params, String email, String mainSalt, String srpSalt, byte[] srpPWBytes, byte[] unwrapBKeyBytes, BigInteger x, BigInteger v) {
    this.params = params;
    this.email = email;
    this.mainSalt = mainSalt;
    this.srpSalt = srpSalt;
    this.srpPWBytes = srpPWBytes;
    this.unwrapBKeyBytes = unwrapBKeyBytes;
    this.x = x;
    this.v = v;
  }

  /**
   * Calculate the SRP verifier <tt>x</tt> value.
   */
  protected static BigInteger x(byte[] emailUTF8, byte[] srpPWBytes, byte[] srpSaltBytes)
      throws NoSuchAlgorithmException, UnsupportedEncodingException {
    byte[] inner = FxAccountUtils.sha256(Utils.concatAll(emailUTF8, ":".getBytes("UTF-8"), srpPWBytes));
    byte[] outer = FxAccountUtils.sha256(Utils.concatAll(srpSaltBytes, inner));
    return new BigInteger(1, outer);
  }

  /**
   * Calculate the SRP verifier <tt>v</tt> value.
   */
  protected static BigInteger v(SRPConstants.Parameters params, BigInteger x)
      throws NoSuchAlgorithmException, UnsupportedEncodingException {
    BigInteger v = params.g.modPow(x, params.N);
    return v;
  }

  public static FxAccount makeFxAccount(String email, String password, String mainSalt, String srpSalt)
      throws UnsupportedEncodingException, GeneralSecurityException {
    final SRPConstants.Parameters params = SRPConstants._2048;
    final ClientSideKeyStretcher keyStretcher = new PICLClientSideKeyStretcher();

    byte[] stretchedPWBytes = keyStretcher.stretch(email, password, null);
    byte[] mainSaltBytes = Utils.hex2Byte(mainSalt);

    byte derived[] = HKDF.derive(stretchedPWBytes, mainSaltBytes, FxAccountUtils.KW("mainKDF"), 2*32);
    byte[] srpPWBytes = new byte[32];
    byte[] unwrapBKeyBytes = new byte[32];
    System.arraycopy(derived, 0, srpPWBytes, 0, 32);
    System.arraycopy(derived, 32, unwrapBKeyBytes, 0, 32);

    byte[] srpSaltBytes = Utils.hex2Byte(srpSalt);
    BigInteger x = x(email.getBytes("UTF-8"), srpPWBytes, srpSaltBytes);
    BigInteger v = v(params, x);

    return new FxAccount(params, email, mainSalt, srpSalt, srpPWBytes, unwrapBKeyBytes, x, v);
  }

  /**
   * Public for testing only.
   */
  public SRPSession srpSession(String srpB, BigInteger a)
      throws UnsupportedEncodingException, GeneralSecurityException {
    BigInteger g = SRPConstants._2048.g;
    BigInteger N = SRPConstants._2048.N;

    BigInteger A = g.modPow(a, N);
    String srpA = SRPConstants._2048.hexModN(A);
    BigInteger B = new BigInteger(srpB, 16);

    // u = H(pad(A) | pad(B))
    byte[] uBytes = FxAccountUtils.sha256(Utils.concatAll(
        Utils.hex2Byte(srpA),
        Utils.hex2Byte(srpB)));
    BigInteger u = new BigInteger(Utils.byte2hex(uBytes), 16);

    // S = (B - k*g^x)^(a + u*x) % N
    // k = H(pad(N) | pad(g))
    byte[] kBytes = FxAccountUtils.sha256(Utils.concatAll(
        Utils.hex2Byte(N.toString(16), SRPConstants._2048.byteLength),
        Utils.hex2Byte(g.toString(16), SRPConstants._2048.byteLength)));
    BigInteger k = new BigInteger(Utils.byte2hex(kBytes), 16);

    BigInteger base = B.subtract(k.multiply(g.modPow(x, N)).mod(N)).mod(N);
    BigInteger pow = a.add(u.multiply(x));
    BigInteger S = base.modPow(pow, N);
    String srpS = SRPConstants._2048.hexModN(S);

    // M = H(pad(A) | pad(B) | pad(S))
    byte[] Mbytes = FxAccountUtils.sha256(Utils.concatAll(
        Utils.hex2Byte(srpA),
        Utils.hex2Byte(srpB),
        Utils.hex2Byte(srpS)));

    // K = H(pad(S))
    byte[] Kbytes = FxAccountUtils.sha256(Utils.hex2Byte(srpS));

    return new SRPSession(a, A, S, Mbytes, Kbytes);
  }

  public SRPSession srpSession(String srpB)
      throws UnsupportedEncodingException, GeneralSecurityException {
    // XXX We should take care to get security to review how we generate a.
    BigInteger a = Utils.generateBigIntegerLessThan(SRPConstants._2048.N);
    return srpSession(srpB, a);
  }
}
