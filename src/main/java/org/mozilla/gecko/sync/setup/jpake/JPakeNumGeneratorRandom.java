package org.mozilla.gecko.sync.setup.jpake;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Helper Function to generate a uniformly random value in [0, r).
 */
public class JPakeNumGeneratorRandom implements JPakeNumGenerator {

  @Override
  public BigInteger generateFromRange(BigInteger r) {
    int maxBytes = (int) Math.ceil(((double) r.bitLength()) / 8);

    byte[] bytes = new byte[maxBytes];
    new SecureRandom().nextBytes(bytes);
    BigInteger randInt = new BigInteger(bytes);
    // TODO: is this going to be very slow?
    // add some bit shifting/masking to decrease mod computation
    return randInt.mod(r);
  }

}
