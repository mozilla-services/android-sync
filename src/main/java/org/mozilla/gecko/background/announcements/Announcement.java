/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.announcements;

import java.net.URI;
import java.net.URISyntaxException;

import org.mozilla.gecko.sync.ExtendedJSONObject;

/**
 * Represents a retrieved product announcement.
 *
 * Instances of this class are immutable.
 */
public class Announcement {
  private static final String KEY_ID    = "id";
  private static final String KEY_TITLE = "title";
  private static final String KEY_URL   = "url";
  private static final String KEY_TEXT  = "text";

  private final int id;
  private final String title;
  private final URI uri;
  private final String text;

  public Announcement(int id, String title, String text, URI uri) {
    this.id    = id;
    this.title = title;
    this.uri   = uri;
    this.text  = text;
  }

  public static Announcement parseAnnouncement(ExtendedJSONObject body) throws URISyntaxException, IllegalArgumentException {
    final Integer id = body.getIntegerSafely(KEY_ID);
    if (id == null) {
      throw new IllegalArgumentException("No id provided in JSON.");
    }
    final String title = body.getString(KEY_TITLE);
    if (title == null || title.trim().length() == 0) {
      throw new IllegalArgumentException("Missing or empty announcement title.");
    }
    final String uri = body.getString(KEY_URL);
    if (uri == null) {
      // Empty or otherwise unhappy URI will throw a URISyntaxException.
      throw new IllegalArgumentException("Missing announcement URI.");
    }

    final String text = body.getString(KEY_TEXT);
    if (text == null) {
      throw new IllegalArgumentException("Missing announcement body.");
    }

    return new Announcement(id, title, text, new URI(uri));
  }

  public int getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getText() {
    return text;
  }

  public URI getUri() {
    return uri;
  }

  public ExtendedJSONObject asJSON() {
    ExtendedJSONObject out = new ExtendedJSONObject();
    out.put(KEY_ID,    id);
    out.put(KEY_TITLE, title);
    out.put(KEY_URL,   uri.toASCIIString());
    out.put(KEY_TEXT,  text);
    return out;
  }
}
