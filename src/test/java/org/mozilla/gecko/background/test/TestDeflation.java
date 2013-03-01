/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.test;

import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import junit.framework.Assert;

import org.junit.Test;
import org.mozilla.gecko.background.bagheera.DeflateHelper;

import ch.boye.httpclientandroidlib.HttpEntity;

public class TestDeflation {
  public static final String TEST_BODY_A = "";
  public static final String TEST_BODY_B = "éíôü}ABCDEFGHaaQRSTUVWXYZá{Zá{";
  public static final String TEST_BODY_C = "{}\n";
  public static final String TEST_BODY_D =
      "{éíôü}ABCDEFGHaaQRSTUVWXYZá{Zá{éíôü}ABCDEFGHaaQRSTUVWXYZá{Zá{éíôü}A" +
      "BCDEFGHaaQRSTUVWXYZá{Zá{éíôü}ABCDEFGHaaQRSTUVWXYZá{Zá{éíôü}ABCDEFGH" +
      "aQRSTUVWXYZá{Zá{éíôü}ABCDEFGHaaQRSTUVWXYZá{Zá{}\n";

  public static int reinflateBytes(byte[] input, byte[] output, int inLength) throws DataFormatException {
    final Inflater inflater = new Inflater();
    inflater.setInput(input, 0, inLength);
    int resultLength = inflater.inflate(output);
    inflater.end();
    System.out.println("Reinflating: " + inLength + " => " + resultLength);
    return resultLength;
  }

  @SuppressWarnings("static-method")
  @Test
  public void testDeflateRoundtrip() throws Exception {
    doDeflateRoundtrip(TEST_BODY_A);
    doDeflateRoundtrip(TEST_BODY_B);
    doDeflateRoundtrip(TEST_BODY_C);
    doDeflateRoundtrip(TEST_BODY_D);
  }

  @SuppressWarnings("static-method")
  @Test
  public void testClientRoundtrip() throws Exception {
    doEntityRoundtrip(TEST_BODY_A);
    doEntityRoundtrip(TEST_BODY_B);
    doEntityRoundtrip(TEST_BODY_C);
    doEntityRoundtrip(TEST_BODY_D);
  }

  @SuppressWarnings("static-method")
  @Test
  /**
   * Compare direct deflation to deflation through HttpEntity in DeflateHelper.
   */
  public void testEntityVersusDirect() throws Exception {
    final String in = TEST_BODY_D;
    final byte[] direct = deflateTrimmed(in.getBytes("UTF-8"));
    final byte[] entity = EntityTestHelper.bytesFromEntity(DeflateHelper.deflateBody(in));
    assertEqualArrays(direct, entity);
  }

  /**
   * Deflate the input, returning a new array of the appropriate size.
   */
  private static byte[] deflateTrimmed(byte[] input) {
    final int byteCount = input.length;
    final byte[] deflated = new byte[DeflateHelper.deflateBound(byteCount)];
    final int deflatedLength = DeflateHelper.deflate(input, deflated);
    final byte[] trimmed = Arrays.copyOf(deflated, deflatedLength);
    return trimmed;
  }

  /**
   * Deflate via direct calls to Deflater, then reinflate and verify
   * round-tripping.
   */
  private static void doDeflateRoundtrip(final String in) throws Exception {
    final byte[] input = in.getBytes("UTF-8");
    final int charCount = in.length();
    final int byteCount = input.length;

    // Deflate on short strings requires *more* room. deflateBound takes that
    // into account.
    final byte[] deflated = new byte[DeflateHelper.deflateBound(byteCount)];
    final int deflatedLength = DeflateHelper.deflate(input, deflated);

    System.out.println("Deflated " + byteCount + " bytes (" + charCount +
                       " chars) to " + deflatedLength);
    final byte[] result = new byte[byteCount];
    final int resultLength = reinflateBytes(deflated, result, deflatedLength);
    System.out.println("Got:      " + resultLength);

    final String outputString = new String(result, 0, resultLength, "UTF-8");
    System.out.println("Comparing: " + in);
    System.out.println("Comparing: " + outputString);
    Assert.assertEquals(in, outputString);
  }

  private static void doEntityRoundtrip(final String in) throws Exception {
    final HttpEntity entity = DeflateHelper.deflateBody(in);
    final int contentLength = (int) entity.getContentLength();

    final byte[] result = new byte[in.length() + 36];     // We cheat.
    final byte[] bytes = EntityTestHelper.bytesFromEntity(entity);

    final int resultLength = reinflateBytes(bytes, result, contentLength);
    System.out.println("Entity: " + Arrays.toString(bytes));
    System.out.println("Got:    " + resultLength);

    final String outputString = new String(result, 0, resultLength, "UTF-8");
    System.out.println("Comparing: " + in);
    System.out.println("Comparing: " + outputString);
    Assert.assertEquals(in, outputString);
  }

  private static void assertEqualArrays(byte[] a, byte[] b) {
    System.out.println("A: " + Arrays.toString(a));
    System.out.println("B: " + Arrays.toString(b));
    Assert.assertTrue(Arrays.equals(a, b));
  }
}
