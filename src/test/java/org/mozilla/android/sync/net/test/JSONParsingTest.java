/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.net.test;

import static org.junit.Assert.*;

import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;

public class JSONParsingTest {

  public static String exampleJSON = "{\"modified\":1233702554.25,\"success\":[\"{GXS58IDC}12\",\"{GXS58IDC}13\",\"{GXS58IDC}15\",\"{GXS58IDC}16\",\"{GXS58IDC}18\",\"{GXS58IDC}19\"],\"failed\":{\"{GXS58IDC}11\":[\"invalid parentid\"],\"{GXS58IDC}14\":[\"invalid parentid\"],\"{GXS58IDC}17\":[\"invalid parentid\"],\"{GXS58IDC}20\":[\"invalid parentid\"]}}";
  public static String exampleIntegral = "{\"modified\":1233702554,}";

  @Test
  public void testFractional() throws IOException, ParseException, NonObjectJSONException {
    ExtendedJSONObject o = new ExtendedJSONObject(exampleJSON);
    assertTrue(o.containsKey("modified"));
    assertTrue(o.containsKey("success"));
    assertTrue(o.containsKey("failed"));
    assertFalse(o.containsKey(" "));
    assertFalse(o.containsKey(""));
    assertFalse(o.containsKey("foo"));
    assertTrue(o.get("modified") instanceof Number);
    assertTrue(o.get("modified").equals(Double.parseDouble("1233702554.25")));
    assertEquals(new Long(1233702554250L), o.getTimestamp("modified"));
    assertEquals(null, o.getTimestamp("foo"));
  }

  @Test
  public void testIntegral() throws IOException, ParseException, NonObjectJSONException {
    ExtendedJSONObject o = new ExtendedJSONObject(exampleIntegral);
    assertTrue(o.containsKey("modified"));
    assertFalse(o.containsKey("success"));
    assertTrue(o.get("modified") instanceof Number);
    assertTrue(o.get("modified").equals(Long.parseLong("1233702554")));
    assertEquals(new Long(1233702554000L), o.getTimestamp("modified"));
    assertEquals(null, o.getTimestamp("foo"));
  }
}
