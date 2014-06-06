/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko;

import java.util.Locale;

import org.mozilla.gecko.background.common.log.Logger;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

/**
 * This is a stub implementation to allow use in android-sync.
 */
public class BrowserLocaleManager implements LocaleManager {
    private static final String LOG_TAG = "BrowserLocaleManager";

    @Override
    public void initialize(Context context) {
      Logger.info(LOG_TAG, "Stub: initialize.");
    }

    @Override
    public String getAndApplyPersistedLocale(Context context) {
      Logger.info(LOG_TAG, "Stub: getAndApplyPersistedLocale.");
      return Locale.getDefault().toString();
    }

    @Override
    public void correctLocale(Context context, Resources resources,
                              Configuration newConfig) {
      Logger.info(LOG_TAG, "Stub: correctLocale.");
    }

    @Override
    public String setSelectedLocale(Context context, String localeCode) {
      Logger.info(LOG_TAG, "Stub: setSelectedLocale: " + localeCode + ".");
      return Locale.getDefault().toString();
    }

    @Override
    public boolean systemLocaleDidChange() {
      Logger.info(LOG_TAG, "Stub: systemLocaleDidChange.");
      return false;
    }

    public static LocaleManager getInstance() {
      return new BrowserLocaleManager();
    }

    @Override
    public void updateConfiguration(Context context, Locale locale) {
    }

    @Override
    public Locale getCurrentLocale(Context context) {
      Logger.info(LOG_TAG, "Stub: getCurrentLocale.");
      return Locale.getDefault();
    }

    @Override
    public void resetToSystemLocale(Context context) {
      Logger.info(LOG_TAG, "Stub: resetToSystemLocale.");
    }

    @Override
    public Locale onSystemConfigurationChanged(Context context,
                                              Resources resources,
                                              Configuration configuration,
                                              Locale currentActivityLocale) {
      return null;
    }

    @Override
    public boolean isEnabled() {
      return false;
    }
}
