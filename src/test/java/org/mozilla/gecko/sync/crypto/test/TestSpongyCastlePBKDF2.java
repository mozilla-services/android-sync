/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.crypto.test;

public class TestSpongyCastlePBKDF2 extends TestPBKDF2 {
  public TestSpongyCastlePBKDF2() {
    super(new SpongyCastlePBKDF2());
  }
}
