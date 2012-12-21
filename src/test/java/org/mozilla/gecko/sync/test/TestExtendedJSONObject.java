/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonArrayJSONException;

public class TestExtendedJSONObject {
  @Test
  public void testParseJSONArray() throws Exception {
    JSONArray result = ExtendedJSONObject.parseJSONArray("[0, 1, {\"test\": 2}]");
    assertNotNull(result);

    assertThat((Long) result.get(0), is(equalTo(0L)));
    assertThat((Long) result.get(1), is(equalTo(1L)));
    assertThat((Long) ((JSONObject) result.get(2)).get("test"), is(equalTo(2L)));
  }

  @Test
  public void testBadParseJSONArray() throws Exception {
    try {
      ExtendedJSONObject.parseJSONArray("[0, ");
      fail();
    } catch (ParseException e) {
      // Do nothing.
    }

    try {
      ExtendedJSONObject.parseJSONArray("{}");
      fail();
    } catch (NonArrayJSONException e) {
      // Do nothing.
    }
  }

  @Test
  public void testParseUTF8AsJSONObject() throws Exception {
    String TEST = "{\"key\":\"value\"}";

    ExtendedJSONObject o = ExtendedJSONObject.parseUTF8AsJSONObject(TEST.getBytes("UTF-8"));
    assertNotNull(o);
    assertEquals("value", o.getString("key"));
  }

  @Test
  public void testBadParseUTF8AsJSONObject() throws Exception {
    try {
      ExtendedJSONObject.parseUTF8AsJSONObject("{}".getBytes("UTF-16"));
      fail();
    } catch (ParseException e) {
      // Do nothing.
    }

    try {
      ExtendedJSONObject.parseUTF8AsJSONObject("{".getBytes("UTF-8"));
      fail();
    } catch (ParseException e) {
      // Do nothing.
    }
  }
}
