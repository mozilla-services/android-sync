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
    assertEquals("sync.prefs.ore7dlrwqi6xr7honxdtpvmh6tly4r7k", Utils.getPrefsPath("foobar@baz.com", "test.url.com"));
    assertEquals("sync.prefs.ore7dlrwqi6xr7honxdtpvmh6tly4r7k", Utils.getPrefsPath("FooBar@Baz.com", "test.url.com"));
    assertEquals("sync.prefs.ore7dlrwqi6xr7honxdtpvmh6tly4r7k", Utils.getPrefsPath("xee7ffonluzpdp66l6xgpyh2v2w6ojkc", "test.url.com"));
  }
}
