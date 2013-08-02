/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.crypto.test;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import org.junit.Test;

import com.lambdaworks.crypto.SCrypt;

public class TestScrypt {

  private void checkScrypt(String passwd, String salt, int N, int r, int p, int dkLen, final String expectedStr)
      throws UnsupportedEncodingException, GeneralSecurityException {
    byte[] scrypt = SCrypt.scrypt(passwd.getBytes("ASCII"), salt.getBytes("ASCII"), N, r, p, dkLen);
    TestPBKDF2.assertExpectedBytes(expectedStr, scrypt);
  }

  @Test
  public void testA() throws Exception {
    checkScrypt("", "", 16, 1, 1, 64, "" +
        "77d6576238657b203b19ca42c18a0497" +
        "f16b4844e3074ae8dfdffa3fede21442" +
        "fcd0069ded0948f8326a753a0fc81f17" +
        "e8d3e0fb2e0d3628cf35e20c38d18906");
  }

  @Test
  public void testB() throws Exception {
    checkScrypt("password", "NaCl", 1024, 8, 16, 64, "" +
        "fdbabe1c9d3472007856e7190d01e9fe" +
        "7c6ad7cbc8237830e77376634b373162" +
        "2eaf30d92e22a3886ff109279d9830da" +
        "c727afb94a83ee6d8360cbdfa2cc0640");
  }

  @Test
  public void testC() throws Exception {
    checkScrypt("pleaseletmein", "SodiumChloride", 16384, 8, 1, 64, "" +
        "7023bdcb3afd7348461c06cd81fd38eb" +
        "fda8fbba904f8e3ea9b543f6545da1f2" +
        "d5432955613f0fcf62d49705242a9af9" +
        "e61e85dc0d651e40dfcf017b45575887");
  }

  @Test
  public void testD() throws Exception {
    checkScrypt("pleaseletmein", "SodiumChloride", 1048576, 8, 1, 64, "" +
        "2101cb9b6a511aaeaddbbe09cf70f881" +
        "ec568d574a2ffd4dabe5ee9820adaa47" +
        "8e56fd8f4ba5d09ffa1c6d927c40f4c3" +
        "37304049e8a952fbcbf45c6fa77a41a4");
  }
}
