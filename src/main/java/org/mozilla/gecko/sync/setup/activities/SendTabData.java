/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.setup.activities;

import android.content.Intent;
import android.os.Bundle;

public class SendTabData {
  public final String title;
  public final String uri;

  public SendTabData(String title, String uri) {
    this.title = title;
    this.uri = uri;
  }

  public static SendTabData fromBundle(Bundle bundle) {
    if (bundle == null) {
      throw new IllegalArgumentException("bundle must not be null");
    }

    String text = bundle.getString(Intent.EXTRA_TEXT);
    String subject = bundle.getString(Intent.EXTRA_SUBJECT);
    String title = bundle.getString(Intent.EXTRA_TITLE);

    // Prefer EXTRA_SUBJECT but accept EXTRA_TITLE.
    String theTitle = subject;
    if (theTitle == null) {
      theTitle = title;
    }

    String theUri = null;
    if (text != null && WebURLFinder.isWebURL(text)) {
      // Given a URL directly (Firefox for Android, Android Browser).
      theUri = text;
    } else {
      // Take first URL in concatenation of EXTRA_TEXT, EXTRA_SUBJECT, and EXTRA_TITLE.
      StringBuffer sb = new StringBuffer();
      for (String data : new String[] { text, subject, title }) {
        if (data != null) {
          sb.append(data);
          sb.append("\n\n");
        }
      }

      // Find first URL.
      theUri = new WebURLFinder(sb.toString()).bestWebURL();
    }

    return new SendTabData(theTitle, theUri);
  }

  public static SendTabData fromIntent(Intent intent) {
    if (intent == null) {
      throw new IllegalArgumentException("intent must not be null");
    }

    return fromBundle(intent.getExtras());
  }
}
