/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.crypto.test;

import org.mozilla.gecko.sync.crypto.JavaPBKDF2;

public class TestJavaPBKDF2 extends TestPBKDF2 {
  public TestJavaPBKDF2() {
    super(new JavaPBKDF2());
  }
}
