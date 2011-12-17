package org.mozilla.gecko.sync.jpake;

import java.math.BigInteger;

public interface JPakeNumGenerator {

  public BigInteger generateFromRange(BigInteger r);
}
