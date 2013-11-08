/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.fxa.test;

import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.gecko.background.fxa.FxAccountUtils;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.net.SRPConstants;

public class TestFxAccountUtils {
  protected static void assertEncoding(String base16String, String utf8String) throws Exception {
    Assert.assertEquals(base16String, FxAccountUtils.bytes(utf8String));
  }

  /**
   * Test vectors from <a href="https://wiki.mozilla.org/Identity/AttachedServices/KeyServerProtocol#stretch-KDF">https://wiki.mozilla.org/Identity/AttachedServices/KeyServerProtocol#stretch-KDF</a>.
   */
  @Test
  public void testUTF8Encoding() throws Exception {
    assertEncoding("616e6472c3a9406578616d706c652e6f7267", "andré@example.org");
    assertEncoding("70c3a4737377c3b67264", "pässwörd");
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

  @Test
  public void testSRPVerifierFunctions() throws Exception {
    byte[] emailUTF8Bytes = Utils.hex2Byte("616e6472c3a9406578616d706c652e6f7267");
    byte[] srpPWBytes = Utils.hex2Byte("00f9b71800ab5337d51177d8fbc682a3653fa6dae5b87628eeec43a18af59a9d", 32);
    byte[] srpSaltBytes = Utils.hex2Byte("00f1000000000000000000000000000000000000000000000000000000000179", 32);

    String expectedX = "81925186909189958012481408070938147619474993903899664126296984459627523279550";
    BigInteger x = FxAccountUtils.srpVerifierLowercaseX(emailUTF8Bytes, srpPWBytes, srpSaltBytes);
    Assert.assertEquals(expectedX, x.toString(10));

    String expectedV = "11464957230405843056840989945621595830717843959177257412217395741657995431613430369165714029818141919887853709633756255809680435884948698492811770122091692817955078535761033207000504846365974552196983218225819721112680718485091921646083608065626264424771606096544316730881455897489989950697705196721477608178869100211706638584538751009854562396937282582855620488967259498367841284829152987988548996842770025110751388952323221706639434861071834212055174768483159061566055471366772641252573641352721966728239512914666806496255304380341487975080159076396759492553066357163103546373216130193328802116982288883318596822";
    BigInteger v = FxAccountUtils.srpVerifierLowercaseV(emailUTF8Bytes, srpPWBytes, srpSaltBytes, SRPConstants._2048.g, SRPConstants._2048.N);
    Assert.assertEquals(expectedV, v.toString(10));
  }
}
