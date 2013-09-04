/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.crypto.test;

import org.mozilla.gecko.sync.crypto.PBKDF2;
import org.spongycastle.crypto.digests.SHA1Digest;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.params.KeyParameter;

public class SpongyCastlePBKDF2 implements PBKDF2 {
  @Override
  public byte[] pbkdf2SHA1(byte[] p, byte[] s, int c, int dkLen) {
    PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA1Digest());
    generator.init(p, s, c);
    KeyParameter key = (KeyParameter)generator.generateDerivedMacParameters(8*dkLen);
    return key.getKey();
  }

  @Override
  public byte[] pbkdf2SHA256(byte[] p, byte[] s, int c, int dkLen) {
    PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
    generator.init(p, s, c);
    KeyParameter key = (KeyParameter)generator.generateDerivedMacParameters(8*dkLen);
    return key.getKey();
  }
}
