/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.crypto.test;

import org.mozilla.gecko.sync.crypto.Scrypt;

public class SpongyCastleScrypt implements Scrypt {
  @Override
  public byte[] scrypt(byte[] password, byte[] salt, int N, int r, int p, int dkLen) {
    return org.spongycastle.crypto.generators.SCrypt.generate(password, salt, N, r, p, dkLen);
  }
}
