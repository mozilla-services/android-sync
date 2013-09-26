/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.fxa.test;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.gecko.background.fxa.ClientSideKeyStretcher;
import org.mozilla.gecko.background.fxa.PICLClientSideKeyStretcher;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.test.SpongyCastlePBKDF2;
import org.mozilla.gecko.sync.crypto.test.SpongyCastleScrypt;

/**
 * Test vectors from
 * <a href="https://wiki.mozilla.org/Identity/AttachedServices/KeyServerProtocol#stretch-KDF">https://wiki.mozilla.org/Identity/AttachedServices/KeyServerProtocol#stretch-KDF</a>.
 */
public class TestPICLClientSideKeyStretcher {
  @Test
  public void testStretch() throws Exception {
    final ClientSideKeyStretcher keyStretcher = new PICLClientSideKeyStretcher(new SpongyCastlePBKDF2(), new SpongyCastleScrypt());
    byte[] stretchedPW = keyStretcher.stretch("andré@example.org", "pässwörd", null);
    Assert.assertArrayEquals(Utils.hex2Byte("c16d46c31bee242cb31f916e9e38d60b76431d3f5304549cc75ae4bc20c7108c"), stretchedPW);
  }
}
