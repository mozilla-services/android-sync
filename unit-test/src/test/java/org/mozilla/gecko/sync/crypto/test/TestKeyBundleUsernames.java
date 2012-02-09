package org.mozilla.gecko.sync.crypto.test;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.KeyBundle;

public class TestKeyBundleUsernames {
  @Test
  public void testUsernames() throws NoSuchAlgorithmException, UnsupportedEncodingException {
    assertEquals(Utils.sha1Base32("foobar@baz.com"), "xee7ffonluzpdp66l6xgpyh2v2w6ojkc");
    assertEquals(KeyBundle.usernameFromAccount("foobar@baz.com"), "xee7ffonluzpdp66l6xgpyh2v2w6ojkc");
    assertEquals(KeyBundle.usernameFromAccount("foobar"), "foobar");
    assertEquals(KeyBundle.usernameFromAccount("xee7ffonluzpdp66l6xgpyh2v2w6ojkc"), "xee7ffonluzpdp66l6xgpyh2v2w6ojkc");
  }
}
