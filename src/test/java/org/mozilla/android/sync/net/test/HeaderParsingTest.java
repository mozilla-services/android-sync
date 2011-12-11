package org.mozilla.android.sync.net.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.gecko.sync.net.SyncResponse;

public class HeaderParsingTest {

  @Test
  public void testDecimalSecondsToMilliseconds() {
    assertEquals(SyncResponse.decimalSecondsToMilliseconds(""),         -1);
    assertEquals(SyncResponse.decimalSecondsToMilliseconds("1234.1.1"), -1);
    assertEquals(SyncResponse.decimalSecondsToMilliseconds("1234"),     1234000);
    assertEquals(SyncResponse.decimalSecondsToMilliseconds("1234.123"), 1234123);
    assertEquals(SyncResponse.decimalSecondsToMilliseconds("1234.12"),  1234120);
  }
}
