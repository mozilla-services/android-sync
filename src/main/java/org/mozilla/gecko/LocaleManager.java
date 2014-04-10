/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

public interface LocaleManager {
    void initialize(Context context);
    String getAndApplyPersistedLocale(Context context);
    void correctLocale(Context context, Resources resources, Configuration newConfig);
    String setSelectedLocale(Context context, String localeCode);
    boolean systemLocaleDidChange();
}
