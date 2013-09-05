/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.fxa.test;

import java.math.BigInteger;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.gecko.background.fxa.ClientSideKeyStretcher;
import org.mozilla.gecko.background.fxa.FxAccount;
import org.mozilla.gecko.background.fxa.PICLClientSideKeyStretcher;
import org.mozilla.gecko.background.fxa.SRPSession;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.test.SpongyCastlePBKDF2;
import org.mozilla.gecko.sync.crypto.test.SpongyCastleScrypt;
import org.mozilla.gecko.sync.net.SRPConstants;

/**
 * Some test vectors from <a href="https://wiki.mozilla.org/Identity/AttachedServices/KeyServerProtocol#SRP_Verifier">https://wiki.mozilla.org/Identity/AttachedServices/KeyServerProtocol#SRP_Verifier</a>.
 * <p>
 * We run similar tests with Unicode and regular ASCII emails and passwords,
 * since the picl-idp is not yet a fan of Unicode in email addresses (and it
 * therefore is not easy to actually use the test vectors against the existing
 * implementation).
 */
public class TestFxAccount {
  private static final String TEST_EMAIL = "andre@example.org";
  private static final String TEST_PASSWORD = "password";

  private static final String TEST_EMAIL_UNICODE = "andré@example.org";
  private static final String TEST_PASSWORD_UNICODE = "pässwörd";

  // If these don't stay fixed, we have to delete the account every test.
  private static final String TEST_MAINSALT = "" +
      "00f0000000000000" +
      "0000000000000000" +
      "0000000000000000" +
      "000000000000034d";

  // private static final String TEST_SRPSALT = TEST_MAINSALT;
  private static final String TEST_SRPSALT = "" +
      "00f1000000000000" +
      "0000000000000000" +
      "0000000000000000" +
      "0000000000000179";

  private static final String TEST_SRPX = "" +
      "3bf2fefcedfbf7cac61c51088266a04d0f7424585a5ddd9f57b03a6f5be93f02";
  private static final String TEST_SRPV = "" +
      "846df04471ce68a987f0b699bb9bfbc9e77537fe1b4feec48d13ef459a76f8ed" +
      "8a80a59b7314f4fd26a421855582e7895fcd271a966cb18b5860b2e5f4663de7" +
      "fa615ab2ca09ff141273e77340050c26c5f82a4d81f97e3cbc7151b127a592ad" +
      "b30c8af831c9773a258c0450254e83b2fa0109b0de79280365c4650c55948789" +
      "e0e879b3ba52acae08543e3a5de36bdf54d78c5150e2d5231757d13c2557c151" +
      "ea9058fdcc253ada1097062932082d4cc8245f4f43a4c79a4cbffda83c9ca029" +
      "749f51f24b2212165d334ae35aaf9126cf5c7fc66b87bf269c880b91dd2609b0" +
      "4d2b1199be45db609dc070272c961743c62d55dcbd0f70b58d0b6be2c1c424d2";

  private static final String TEST_SRPX_UNICODE = "" +
      "b5200337cc3f3f926cdddae0b2d31029c069936a844aff58779a545be89d0abe";
  private static final String TEST_SRPV_UNICODE = "" +
      "00173ffa0263e63ccfd6791b8ee2a40f048ec94cd95aa8a3125726f9805e0c82" +
      "83c658dc0b607fbb25db68e68e93f2658483049c68af7e8214c49fde2712a775" +
      "b63e545160d64b00189a86708c69657da7a1678eda0cd79f86b8560ebdb1ffc2" +
      "21db360eab901d643a75bf1205070a5791230ae56466b8c3c1eb656e19b794f1" +
      "ea0d2a077b3a755350208ea0118fec8c4b2ec344a05c66ae1449b32609ca7189" +
      "451c259d65bd15b34d8729afdb5faff8af1f3437bbdc0c3d0b069a8ab2a959c9" +
      "0c5a43d42082c77490f3afcc10ef5648625c0605cdaace6c6fdc9e9a7e6635d6" +
      "19f50af7734522470502cab26a52a198f5b00a279858916507b0b4e9ef9524d6";

  private static final String TEST_SRPB_UNICODE = "" +
      "0022ce5a7b9d81277172caa20b0f1efb4643b3becc53566473959b07b790d3c3" +
      "f08650d5531c19ad30ebb67bdb481d1d9cf61bf272f8439848fdda58a4e6abc5" +
      "abb2ac496da5098d5cbf90e29b4b110e4e2c033c70af73925fa37457ee13ea3e" +
      "8fde4ab516dff1c2ae8e57a6b264fb9db637eeeae9b5e43dfaba9b329d3b8770" +
      "ce89888709e026270e474eef822436e6397562f284778673a1a7bc12b6883d1c" +
      "21fbc27ffb3dbeb85efda279a69a19414969113f10451603065f0a0126666456" +
      "51dde44a52f4d8de113e2131321df1bf4369d2585364f9e536c39a4dce33221b" +
      "e57d50ddccb4384e3612bbfd03a268a36e4f7e01de651401e108cc247db50392";

  private static final String TEST_SRPS_UNICODE = "" +
      "0092aaf0f527906aa5e8601f5d707907a03137e1b601e04b5a1deb02a981f4be" +
      "037b39829a27dba50f1b27545ff2e28729c2b79dcbdd32c9d6b20d340affab91" +
      "a626a8075806c26fe39df91d0ad979f9b2ee8aad1bc783e7097407b63bfe58d9" +
      "118b9b0b2a7c5c4cdebaf8e9a460f4bf6247b0da34b760a59fac891757ddedca" +
      "f08eed823b090586c63009b2d740cc9f5397be89a2c32cdcfe6d6251ce11e44e" +
      "6ecbdd9b6d93f30e90896d2527564c7eb9ff70aa91acc0bac1740a11cd184ffb" +
      "989554ab58117c2196b353d70c356160100ef5f4c28d19f6e59ea2508e8e8aac" +
      "6001497c27f362edbafb25e0f045bfdf9fb02db9c908f10340a639fe84c31b27";
  private static final String TEST_SRPM_UNICODE = "" +
      "27949ec1e0f1625633436865edb037e23eb6bf5cb91873f2a2729373c2039008";
  private static final String TEST_SRPK_UNICODE = "" +
      "e68fd0112bfa31dcffc8e9c96a1cbadb4c3145978ff35c73e5bf8d30bbc7499a";

  protected ClientSideKeyStretcher keyStretcher;

  @Before
  public void setUp() {
    keyStretcher = new PICLClientSideKeyStretcher(
        new SpongyCastlePBKDF2(),
        new SpongyCastleScrypt());
  }

  @Test
  public void testMakeFxAccount() throws Exception {
    byte[] stretchedPWBytes = keyStretcher.stretch(TEST_EMAIL, TEST_PASSWORD, null);
    FxAccount account = FxAccount.makeFxAccount(TEST_EMAIL, stretchedPWBytes, TEST_MAINSALT, TEST_SRPSALT);

    Assert.assertEquals(TEST_MAINSALT, account.mainSalt);
    Assert.assertEquals(TEST_SRPSALT, account.srpSalt);
    Assert.assertEquals(TEST_SRPV, SRPConstants._2048.hexModN(account.v));
    Assert.assertEquals(new BigInteger(TEST_SRPX, 16), account.x);
  }

  @Test
  public void testMakeFxAccountUnicode() throws Exception {
    byte[] stretchedPWBytes = keyStretcher.stretch(TEST_EMAIL_UNICODE, TEST_PASSWORD_UNICODE, null);
    FxAccount account = FxAccount.makeFxAccount(TEST_EMAIL_UNICODE, stretchedPWBytes, TEST_MAINSALT, TEST_SRPSALT);

    Assert.assertEquals(TEST_MAINSALT, account.mainSalt);
    Assert.assertEquals(TEST_SRPSALT, account.srpSalt);
    Assert.assertEquals(TEST_SRPV_UNICODE, SRPConstants._2048.hexModN(account.v));
    Assert.assertEquals(new BigInteger(TEST_SRPX_UNICODE, 16), account.x);


    BigInteger a = new BigInteger("" +
        "00f2000000000000000000000000000000000000000000000000000000000000" +
        "0000000000000000000000000000000000000000000000000000000000000000" +
        "0000000000000000000000000000000000000000000000000000000000000000" +
        "0000000000000000000000000000000000000000000000000000000000000000" +
        "0000000000000000000000000000000000000000000000000000000000000000" +
        "0000000000000000000000000000000000000000000000000000000000000000" +
        "0000000000000000000000000000000000000000000000000000000000000000" +
        "000000000000000000000000000000000000000000000000000000000000d3d7", 16);
    SRPSession srpSession = account.srpSession(TEST_SRPB_UNICODE, a);
    Assert.assertEquals(TEST_SRPS_UNICODE, SRPConstants._2048.hexModN(srpSession.S));
    Assert.assertEquals(TEST_SRPM_UNICODE, Utils.byte2hex(srpSession.M));
    Assert.assertEquals(TEST_SRPK_UNICODE, Utils.byte2hex(srpSession.K));
  }
}
