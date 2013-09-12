/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.fxaccount.test;

import org.mozilla.gecko.background.fxaccount.ClientSideKeyStretcher;
import org.mozilla.gecko.background.fxaccount.PICLClientSideKeyStretcher;
import org.mozilla.gecko.sync.Utils;

import android.test.AndroidTestCase;


public class TestPICLClientSideKeyStretcher extends AndroidTestCase {

//    /**
//     *
//     * @param stretchedPW
//     * @param mainSalt
//     * @return
//     * @throws UnsupportedEncodingException
//     * @throws GeneralSecurityException
//     */
//    protected byte[] mainKDF(byte[] stretchedPW, byte[] mainSalt) throws UnsupportedEncodingException, GeneralSecurityException {
//      byte[] derived = HKDF.derive(stretchedPW, mainSalt, FxAccountUtils.KW("mainKDF"), 2*32);
//      return derived;
//    }

//      byte[] srpPW = new byte[32];
//      byte[] unwrapBKey = new byte[32];
//      System.arraycopy(derived, 0, srpPW, 0, 32);
//      System.arraycopy(derived, 32, unwrapBKey, 0, 32);
//
//      ExtendedJSONObject bits = new ExtendedJSONObject();
//      bits.put("mainSalt", Utils.byte2hex(mainSalt));
//      bits.put("srpPW", Utils.byte2hex(srpPW));
//      bits.put("unwrapBKey", Utils.byte2hex(unwrapBKey));
//      return bits;
//    }

  @SuppressWarnings("static-method")
  public void testStretch() throws Exception {
    final ClientSideKeyStretcher keyStretcher = new PICLClientSideKeyStretcher();
    byte[] stretchedPW = keyStretcher.stretch("andré@example.org", "pässwörd", null);
//    Assert.assertEquals("616e6472c3a9406578616d706c652e6f7267", bits.getString("emailUTF8"));
//    Assert.assertEquals("70c3a4737377c3b67264", bits.getString("passwordUTF8"));
//    Assert.assertEquals("f84913e3d8e6d624689d0a3e9678ac8dcc79d2c2f3d9641488cd9d6ef6cd83dd", bits.getString("k1"));
//    Assert.assertEquals("5b82f146a64126923e4167a0350bb181feba61f63cb1714012b19cb0be0119c5", bits.getString("k2"));
    assertEquals("c16d46c31bee242cb31f916e9e38d60b76431d3f5304549cc75ae4bc20c7108c", Utils.byte2hex(stretchedPW));
  }

/*
  @Test
  public void testStretchKDF() throws Exception {
    ExtendedJSONObject bits = stretchKDF("andré@example.org", "pässwörd");
    Assert.assertEquals("616e6472c3a9406578616d706c652e6f7267", bits.getString("emailUTF8"));
    Assert.assertEquals("70c3a4737377c3b67264", bits.getString("passwordUTF8"));
    Assert.assertEquals("f84913e3d8e6d624689d0a3e9678ac8dcc79d2c2f3d9641488cd9d6ef6cd83dd", bits.getString("k1"));
    Assert.assertEquals("5b82f146a64126923e4167a0350bb181feba61f63cb1714012b19cb0be0119c5", bits.getString("k2"));
    Assert.assertEquals("c16d46c31bee242cb31f916e9e38d60b76431d3f5304549cc75ae4bc20c7108c", bits.getString("stretchedPW"));

    byte[] mainSalt = Utils.hex2Byte("00f000000000000000000000000000000000000000000000000000000000034d");
    bits.putAll(mainKDF(Utils.hex2Byte(bits.getString("stretchedPW")), mainSalt));
    Assert.assertEquals("00f9b71800ab5337d51177d8fbc682a3653fa6dae5b87628eeec43a18af59a9d", bits.getString("srpPW"));
    Assert.assertEquals("6ea660be9c89ec355397f89afb282ea0bf21095760c8c5009bbcc894155bbe2a", bits.getString("unwrapBKey"));

    byte[] srpSalt = Utils.hex2Byte("00f1000000000000000000000000000000000000000000000000000000000179");
    Verifier verifier = srpVerifier(srpSalt, Utils.hex2Byte(bits.getString("emailUTF8")), Utils.hex2Byte(bits.getString("srpPW"))); // XXX the salt is misleading in the test vectors.
    bits.put("x", verifier.x.toString(16));
    bits.put("v", verifier.v.toString(16));

    Assert.assertEquals("81925186909189958012481408070938147619474993903899664126296984459627523279550",
        new BigInteger(bits.getString("x"), 16).toString(10));
    Assert.assertEquals("11464957230405843056840989945621595830717843959177257412217395741657995431613430369165714029818141919887853709633756255809680435884948698492811770122091692817955078535761033207000504846365974552196983218225819721112680718485091921646083608065626264424771606096544316730881455897489989950697705196721477608178869100211706638584538751009854562396937282582855620488967259498367841284829152987988548996842770025110751388952323221706639434861071834212055174768483159061566055471366772641252573641352721966728239512914666806496255304380341487975080159076396759492553066357163103546373216130193328802116982288883318596822",
        new BigInteger(bits.getString("v"), 16).toString(10));

    bits.put("srpVerifier", hexModN(verifier.v));
    Assert.assertEquals("00173ffa0263e63ccfd6791b8ee2a40f048ec94cd95aa8a3125726f9805e0c8283c658dc0b607fbb25db68e68e93f2658483049c68af7e8214c49fde2712a775b63e545160d64b00189a86708c69657da7a1678eda0cd79f86b8560ebdb1ffc221db360eab901d643a75bf1205070a5791230ae56466b8c3c1eb656e19b794f1ea0d2a077b3a755350208ea0118fec8c4b2ec344a05c66ae1449b32609ca7189451c259d65bd15b34d8729afdb5faff8af1f3437bbdc0c3d0b069a8ab2a959c90c5a43d42082c77490f3afcc10ef5648625c0605cdaace6c6fdc9e9a7e6635d619f50af7734522470502cab26a52a198f5b00a279858916507b0b4e9ef9524d6",
        bits.getString("srpVerifier"));

    BigInteger g = SRPConstants._2048.g;
    BigInteger N = SRPConstants._2048.N;

    BigInteger a = new BigInteger("00f2000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000d3d7", 16);
    BigInteger A = g.modPow(a, N);
    String srpA = hexModN(A);
    Assert.assertEquals("007da76cb7e77af5ab61f334dbd5a958513afcdf0f47ab99271fc5f7860fe2132e5802ca79d2e5c064bb80a38ee08771c98a937696698d878d78571568c98a1c40cc6e7cb101988a2f9ba3d65679027d4d9068cb8aad6ebff0101bab6d52b5fdfa81d2ed48bba119d4ecdb7f3f478bd236d5749f2275e9484f2d0a9259d05e49d78a23dd26c60bfba04fd346e5146469a8c3f010a627be81c58ded1caaef2363635a45f97ca0d895cc92ace1d09a99d6beb6b0dc0829535c857a419e834db12864cd6ee8a843563b0240520ff0195735cd9d316842d5d3f8ef7209a0bb4b54ad7374d73e79be2c3975632de562c596470bb27bad79c3e2fcddf194e1666cb9fc", srpA);
    String srpB = "0022ce5a7b9d81277172caa20b0f1efb4643b3becc53566473959b07b790d3c3f08650d5531c19ad30ebb67bdb481d1d9cf61bf272f8439848fdda58a4e6abc5abb2ac496da5098d5cbf90e29b4b110e4e2c033c70af73925fa37457ee13ea3e8fde4ab516dff1c2ae8e57a6b264fb9db637eeeae9b5e43dfaba9b329d3b8770ce89888709e026270e474eef822436e6397562f284778673a1a7bc12b6883d1c21fbc27ffb3dbeb85efda279a69a19414969113f10451603065f0a012666645651dde44a52f4d8de113e2131321df1bf4369d2585364f9e536c39a4dce33221be57d50ddccb4384e3612bbfd03a268a36e4f7e01de651401e108cc247db50392";
    BigInteger B = new BigInteger(srpB, 16);

    byte[] uBytes = TestPiclClientKeyStretcher.sha256(Utils.concatAll(
        Utils.hex2Byte(srpA),
        Utils.hex2Byte(srpB)));
    Assert.assertEquals("b284aa1064e8775150da6b5e2147b47ca7df505bed94a6f4bb2ad873332ad732",
        Utils.byte2hex(uBytes));
    BigInteger u = new BigInteger(Utils.byte2hex(uBytes), 16);

    // S = (B - k*g^x)^(a + u*x) % N
    // Public.
    byte[] kBytes = sha256(Utils.concatAll(
        Utils.hex2Byte(N.toString(16), SRPConstants._2048.byteLength),
        Utils.hex2Byte(g.toString(16), SRPConstants._2048.byteLength)));
    BigInteger k = new BigInteger(Utils.byte2hex(kBytes), 16);
    Assert.assertEquals("2590038599070950300691544216303772122846747035652616593381637186118123578112", k.toString(10));

    // Private.
    BigInteger x = verifier.x;
    // BigInteger v = verifier.v;

    BigInteger base = B.subtract(k.multiply(g.modPow(x, N)).mod(N)).mod(N);
    BigInteger pow = a.add(u.multiply(x));
    BigInteger S = base.modPow(pow, N);
    String srpS = hexModN(S);
    Assert.assertEquals("0092aaf0f527906aa5e8601f5d707907a03137e1b601e04b5a1deb02a981f4be037b39829a27dba50f1b27545ff2e28729c2b79dcbdd32c9d6b20d340affab91a626a8075806c26fe39df91d0ad979f9b2ee8aad1bc783e7097407b63bfe58d9118b9b0b2a7c5c4cdebaf8e9a460f4bf6247b0da34b760a59fac891757ddedcaf08eed823b090586c63009b2d740cc9f5397be89a2c32cdcfe6d6251ce11e44e6ecbdd9b6d93f30e90896d2527564c7eb9ff70aa91acc0bac1740a11cd184ffb989554ab58117c2196b353d70c356160100ef5f4c28d19f6e59ea2508e8e8aac6001497c27f362edbafb25e0f045bfdf9fb02db9c908f10340a639fe84c31b27",
        srpS);

    byte[] M1bytes = TestPiclClientKeyStretcher.sha256(Utils.concatAll(
        Utils.hex2Byte(srpA),
        Utils.hex2Byte(srpB),
        Utils.hex2Byte(srpS)));
    String srpM1 = Utils.byte2hex(M1bytes, 32);
    Assert.assertEquals("27949ec1e0f1625633436865edb037e23eb6bf5cb91873f2a2729373c2039008",
        srpM1);

    byte[] Kbytes = TestPiclClientKeyStretcher.sha256(Utils.hex2Byte(srpS));
    String srpK = Utils.byte2hex(Kbytes, 32);
    Assert.assertEquals("e68fd0112bfa31dcffc8e9c96a1cbadb4c3145978ff35c73e5bf8d30bbc7499a",
        srpK);
  }
*/

//mainSalt(normallyrandom):
//
//srpPW:
//
//unwrapBKey:
//
//internalx(base10):
//
//internalx(hex):b5200337cc3f3f926cdddae0b2d31029c069936a844aff58779a545be89d0abe
//
//v(verifierasnumber)(base10):

//
//  public static ExtendedJSONObject bits(String email, String password)
//        throws UnsupportedEncodingException, GeneralSecurityException {
//
//    byte[] srpSalt = Utils.hex2Byte("00f1000000000000000000000000000000000000000000000000000000000179");
//
//    BigInteger g = SRPConstants._2048.g;
//    BigInteger N = SRPConstants._2048.N;
//
//    Foo foo = srpVerifier(srpSalt, emailUTF8, srpPW);
//
//    BigInteger a = new BigInteger("" +
//        "00f20000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "0000000000000000000000000000d3d7", 16);
//    BigInteger A = g.modPow(a, N);
//
//    byte[] kBytes = sha256(Utils.concatAll(
//        Utils.hex2Byte(N.toString(16), SRPConstants._2048.byteLength),
//        Utils.hex2Byte(g.toString(16), SRPConstants._2048.byteLength)));
//    BigInteger k = new BigInteger(1, kBytes);
//
//    ExtendedJSONObject bits = new ExtendedJSONObject();
//    bits.put("k", k.toString(16));
//    bits.put("g", g.toString(16));
//    bits.put("N", N.toString(16));
//    bits.put("x", foo.x.toString(16));
//    bits.put("v", foo.v.toString(16));
//    bits.put("a", a.toString(16));
//    bits.put("A", A.toString(16));
//    bits.put("srpA", Utils.byte2hex(Utils.hex2Byte(A.toString(16)), SRPConstants._2048.hexLength));
//    return bits;
//  }
//
//  private void checkStretch(String email, String password, String expectedStr)
//      throws UnsupportedEncodingException, GeneralSecurityException {
//    // ExtendedJSONObject bits =
////    System.out.println(Utils.byte2hex(k1));
////    System.out.println(Utils.byte2hex(k2));
////    System.out.println(Utils.byte2hex(in));
////    System.out.println(Utils.byte2hex(stretchedPW));
////    System.out.println(Utils.byte2hex(srpPW));
////    System.out.println(Utils.byte2hex(unwrapBKey));
////    printhex("srpVerifier", v);
////    printhex("srpA", A);
////    TestPBKDF2.assertExpectedBytes(expectedStr, stretchedPW);
////    System.out.println(Utils.byte2hex(xBytes(srpSalt, emailUTF8, srpPW)));
//
//    BigInteger b = new BigInteger("" +
//        "00f30000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "00000000000000000000000000000000" +
//        "0000000000000000000000000000000f", 16);
//
//    // B = k*v + g^b
////    BigInteger B = (
////        (k.multiply(v)).mod(N)
////        .add(g.modPow(b, N))
////        ).mod(N);
////    printhex("srpB", B);
//  }
//
//  @Test
//  public void test() throws Exception {
//    checkStretch("andré@example.org", "pässwörd", "" +
//        "c16d46c31bee242c" +
//        "b31f916e9e38d60b" +
//        "76431d3f5304549c" +
//        "c75ae4bc20c7108c");
//    //        stretchedPW = pbkdf2_bin(k2+passwordUTF8, KWE("second-PBKDF", emailUTF8),
//    //                                 20*1000, keylen=1*32, hashfunc=sha256)
//  }
}
