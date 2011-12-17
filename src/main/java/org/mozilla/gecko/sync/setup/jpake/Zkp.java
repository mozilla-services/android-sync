package org.mozilla.gecko.sync.setup.jpake;

import java.math.BigInteger;

public class Zkp {
  public BigInteger gr;
  public BigInteger b;
  public String id;

  public Zkp(BigInteger gr, BigInteger b, String id) {
    this.gr = gr;
    this.b = b;
    this.id = id;
  }
}
