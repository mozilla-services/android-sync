/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.crypto.test;

public class TestSpongyCastleScrypt extends TestScrypt {
  public TestSpongyCastleScrypt() {
    super(new SpongyCastleScrypt());
  }
}
