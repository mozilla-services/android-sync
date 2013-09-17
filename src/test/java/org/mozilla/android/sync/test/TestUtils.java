/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.junit.Test;
import org.mozilla.gecko.sync.Utils;

public class TestUtils extends Utils {

  @Test
  public void testGenerateGUID() {
    for (int i = 0; i < 1000; ++i) {
      assertEquals(12, Utils.generateGuid().length());
    }
  }

  @Test
  public void testToCommaSeparatedString() {
    ArrayList<String> xs = new ArrayList<String>();
    assertEquals("", Utils.toCommaSeparatedString(null));
    assertEquals("", Utils.toCommaSeparatedString(xs));
    xs.add("test1");
    assertEquals("test1", Utils.toCommaSeparatedString(xs));
    xs.add("test2");
    assertEquals("test1, test2", Utils.toCommaSeparatedString(xs));
    xs.add("test3");
    assertEquals("test1, test2, test3", Utils.toCommaSeparatedString(xs));
  }

  @Test
  public void testUsernameFromAccount() throws NoSuchAlgorithmException, UnsupportedEncodingException {
    assertEquals("xee7ffonluzpdp66l6xgpyh2v2w6ojkc", Utils.sha1Base32("foobar@baz.com"));
    assertEquals("xee7ffonluzpdp66l6xgpyh2v2w6ojkc", Utils.usernameFromAccount("foobar@baz.com"));
    assertEquals("xee7ffonluzpdp66l6xgpyh2v2w6ojkc", Utils.usernameFromAccount("FooBar@Baz.com"));
    assertEquals("xee7ffonluzpdp66l6xgpyh2v2w6ojkc", Utils.usernameFromAccount("xee7ffonluzpdp66l6xgpyh2v2w6ojkc"));
    assertEquals("foobar",                           Utils.usernameFromAccount("foobar"));
    assertEquals("foobar",                           Utils.usernameFromAccount("FOOBAr"));
  }

  @Test
  public void testGetPrefsPath() throws NoSuchAlgorithmException, UnsupportedEncodingException {
    assertEquals("ore7dlrwqi6xr7honxdtpvmh6tly4r7k", Utils.sha1Base32("test.url.com:xee7ffonluzpdp66l6xgpyh2v2w6ojkc"));

    assertEquals("sync.prefs.ore7dlrwqi6xr7honxdtpvmh6tly4r7k", Utils.getPrefsPath("product", "foobar@baz.com", "test.url.com", "default", 0));
    assertEquals("sync.prefs.ore7dlrwqi6xr7honxdtpvmh6tly4r7k", Utils.getPrefsPath("org.mozilla.firefox_beta", "FooBar@Baz.com", "test.url.com", "default", 0));
    assertEquals("sync.prefs.ore7dlrwqi6xr7honxdtpvmh6tly4r7k", Utils.getPrefsPath("org.mozilla.firefox", "xee7ffonluzpdp66l6xgpyh2v2w6ojkc", "test.url.com", "profile", 0));

    assertEquals("sync.prefs.product.ore7dlrwqi6xr7honxdtpvmh6tly4r7k.default.1", Utils.getPrefsPath("product", "foobar@baz.com", "test.url.com", "default", 1));
    assertEquals("sync.prefs.with!spaces_underbars!periods.ore7dlrwqi6xr7honxdtpvmh6tly4r7k.default.1", Utils.getPrefsPath("with spaces_underbars.periods", "foobar@baz.com", "test.url.com", "default", 1));
    assertEquals("sync.prefs.org!mozilla!firefox_beta.ore7dlrwqi6xr7honxdtpvmh6tly4r7k.default.2", Utils.getPrefsPath("org.mozilla.firefox_beta", "FooBar@Baz.com", "test.url.com", "default", 2));
    assertEquals("sync.prefs.org!mozilla!firefox.ore7dlrwqi6xr7honxdtpvmh6tly4r7k.profile.3", Utils.getPrefsPath("org.mozilla.firefox", "xee7ffonluzpdp66l6xgpyh2v2w6ojkc", "test.url.com", "profile", 3));
  }

  @Test
  public void testObfuscateEmail() {
    assertEquals("XXX@XXX.XXX", Utils.obfuscateEmail("foo@bar.com"));
    assertEquals("XXXX@XXX.XXXX.XX", Utils.obfuscateEmail("foot@bar.test.ca"));
  }

  @Test
  public void testByte2Hex() {
    assertEquals("00017fff", Utils.byte2hex(new byte[] { 0, 1, 127, (byte) 255 }));
    assertEquals("000017fff", Utils.byte2hex(new byte[] { 0, 1, 127, (byte) 255 }, 9));
    assertEquals("0000017fff", Utils.byte2hex(new byte[] { 0, 1, 127, (byte) 255 }, 10));
  }
}
