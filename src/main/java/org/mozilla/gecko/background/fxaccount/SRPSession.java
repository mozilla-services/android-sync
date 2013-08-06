/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.fxaccount;

import java.math.BigInteger;

public class SRPSession {
  public final BigInteger a;
  public final BigInteger A;
  public final BigInteger S;
  public final byte[] M;
  public final byte[] K;

  public SRPSession(BigInteger a, BigInteger A, BigInteger S, byte[] M, byte[] K) {
    this.a = a;
    this.A = A;
    this.S = S;
    this.M = M;
    this.K = K;
  }
}
