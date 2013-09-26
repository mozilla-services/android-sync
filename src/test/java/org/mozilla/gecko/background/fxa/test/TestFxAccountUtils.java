/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.fxa.test;

import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;

public class TestFxAccountUtils {
  protected static void assertEncoding(String utf8String, String base16String) throws Exception {
    byte[] bytes = utf8String.getBytes("UTF-8");
    BigInteger bi = new BigInteger(base16String, 16);
    Assert.assertArrayEquals(bi.toByteArray(), bytes);
  }

  /**
   * Test vectors from <a href="https://wiki.mozilla.org/Identity/AttachedServices/KeyServerProtocol#stretch-KDF">https://wiki.mozilla.org/Identity/AttachedServices/KeyServerProtocol#stretch-KDF</a>.
   */
  @Test
  public void testUTF8Encoding() throws Exception {
    assertEncoding("andré@example.org", "616e6472c3a9406578616d706c652e6f7267");
    assertEncoding("pässwörd", "70c3a4737377c3b67264");
  }
}
