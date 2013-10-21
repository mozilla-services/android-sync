/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.fxa.test;

import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.gecko.background.fxa.FxAccountUtils;

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

  @Test
  public void testHexModN() {
    BigInteger N = BigInteger.valueOf(14);
    Assert.assertEquals(4, N.bitLength());
    Assert.assertEquals(1, (N.bitLength() + 7)/8);
    Assert.assertEquals("00", FxAccountUtils.hexModN(BigInteger.valueOf(0), N));
    Assert.assertEquals("05", FxAccountUtils.hexModN(BigInteger.valueOf(5), N));
    Assert.assertEquals("0b", FxAccountUtils.hexModN(BigInteger.valueOf(11), N));
    Assert.assertEquals("00", FxAccountUtils.hexModN(BigInteger.valueOf(14), N));
    Assert.assertEquals("01", FxAccountUtils.hexModN(BigInteger.valueOf(15), N));
    Assert.assertEquals("02", FxAccountUtils.hexModN(BigInteger.valueOf(16), N));
    Assert.assertEquals("02", FxAccountUtils.hexModN(BigInteger.valueOf(30), N));

    N = BigInteger.valueOf(260);
    Assert.assertEquals("00ff", FxAccountUtils.hexModN(BigInteger.valueOf(255), N));
    Assert.assertEquals("0100", FxAccountUtils.hexModN(BigInteger.valueOf(256), N));
    Assert.assertEquals("0101", FxAccountUtils.hexModN(BigInteger.valueOf(257), N));
    Assert.assertEquals("0001", FxAccountUtils.hexModN(BigInteger.valueOf(261), N));
  }
}
