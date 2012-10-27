/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.test;

import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.gecko.background.announcements.Announcement;
import org.mozilla.gecko.sync.ExtendedJSONObject;

public class TestAnnouncements {
  public static final String GOOD = "{" +
      "   \"id\": \"12345678\"," +
      "   \"url\": \"https://campaigns.mozilla.org/en_US/9j2q98fdqjdaff3ar\"," +
      "   \"title\": \"Get awesome stuff\"," +
      "   \"text\": \"It's awesome\"" +
      "}";

  public static final String MISSING_URL = "{" +
      "   \"id\": \"12345678\"," +
      "   \"title\": \"Get awesome stuff\"," +
      "   \"text\": \"It's awesome\"" +
      "}";

  public static final String MISSING_TITLE = "{" +
      "   \"id\": \"12345678\"," +
      "   \"url\": \"https://campaigns.mozilla.org/en_US/9j2q98fdqjdaff3ar\"," +
      "   \"text\": \"It's awesome\"" +
      "}";

  public static final String MISSING_TEXT = "{" +
      "   \"id\": \"12345678\"," +
      "   \"url\": \"https://campaigns.mozilla.org/en_US/9j2q98fdqjdaff3ar\"," +
      "   \"title\": \"Get awesome stuff\"," +
      "}";

  public static final String BAD_URL = "{" +
      "   \"id\": \"12345678\"," +
      "   \"url\": \"https://\"," +
      "   \"title\": \"Get awesome stuff\"," +
      "   \"text\": \"It's awesome\"" +
      "}";

  public static final String FORBIDDEN_URL = "{" +
      "   \"id\": \"666666\"," +
      "   \"url\": \"file:///some/script\"," +
      "   \"title\": \"Get awful stuff\"," +
      "   \"text\": \"It's awful\"" +
      "}";

  private static Announcement expectSuccess(final String json) {
    try {
      return Announcement.parseAnnouncement(ExtendedJSONObject.parseJSONObject(json));
    } catch (Exception e) {
      Assert.fail("Got exception parsing: " + e);
      return null;
    }
  }

  private static void expectFailure(final String json) {
    final ExtendedJSONObject parsed;
    try {
       parsed = ExtendedJSONObject.parseJSONObject(json);
    } catch (Exception e) {
      Assert.fail("Got unexpected JSON parse exception.");
      return;
    }

    try {
      Announcement.parseAnnouncement(parsed);
      Assert.fail("Was expecting failure, got success.");
    } catch (IllegalArgumentException e) {
      return;
    } catch (URISyntaxException e) {
      return;
    }
  }

  private static void expectValid(final Announcement announce) {
    Assert.assertTrue(Announcement.isValidAnnouncement(announce));
  }

  private static void expectInvalid(final Announcement announce) {
    Assert.assertFalse(Announcement.isValidAnnouncement(announce));
  }

  @SuppressWarnings("static-method")
  @Test
  public void testAnnouncementParsing() {
    expectValid(expectSuccess(GOOD));
    expectInvalid(expectSuccess(FORBIDDEN_URL));
    expectFailure(MISSING_URL);
    expectFailure(MISSING_TITLE);
    expectFailure(MISSING_TEXT);
    expectFailure(BAD_URL);
  }
}
