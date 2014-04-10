/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.setup.activities;

import org.mozilla.gecko.BrowserLocaleManager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;

public class LocaleAware {
  @TargetApi(Build.VERSION_CODES.GINGERBREAD)
  protected static void initializeLocale(Context context) {
    final BrowserLocaleManager localeManager = BrowserLocaleManager.getInstance();
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
      localeManager.getAndApplyPersistedLocale(context);
    } else {
      final StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
      StrictMode.allowThreadDiskWrites();
      try {
        localeManager.getAndApplyPersistedLocale(context);
      } finally {
        StrictMode.setThreadPolicy(savedPolicy);
      }
    }
  }

  public static class LocaleAwareFragmentActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      LocaleAware.initializeLocale(getApplicationContext());
    }
  }

  public static class LocaleAwareActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      LocaleAware.initializeLocale(getApplicationContext());
    }
  }
}