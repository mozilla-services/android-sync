/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.fxa.test;

import java.util.Calendar;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.gecko.background.fxa.FxAccountAgeLockoutHelper;
import org.mozilla.gecko.fxa.FxAccountConstants;

public class TestFxAccountAgeLockoutHelper {
  @Test
  public void testPassesAgeCheck() {
    Calendar today = Calendar.getInstance();
    int birthMonthIndex = today.get(Calendar.MONTH);
    int birthDate = today.get(Calendar.DATE);
    int birthYear = today.get(Calendar.YEAR) - FxAccountConstants.MINIMUM_AGE_TO_CREATE_AN_ACCOUNT;
    Assert.assertTrue("Minimum age as of today",
        FxAccountAgeLockoutHelper.passesAgeCheck(birthYear, birthMonthIndex, birthDate));

    Calendar yesterday = Calendar.getInstance();
    yesterday.add(Calendar.DATE, -1);
    birthMonthIndex = yesterday.get(Calendar.MONTH);
    birthDate = yesterday.get(Calendar.DATE);
    birthYear = yesterday.get(Calendar.YEAR) - FxAccountConstants.MINIMUM_AGE_TO_CREATE_AN_ACCOUNT;
    Assert.assertTrue("Minimum age more than by a day",
        FxAccountAgeLockoutHelper.passesAgeCheck(birthYear, birthMonthIndex, birthDate));

    Calendar tomorrow = Calendar.getInstance();
    tomorrow.add(Calendar.DATE, 1);
    birthMonthIndex = tomorrow.get(Calendar.MONTH);
    birthDate = tomorrow.get(Calendar.DATE);
    birthYear = tomorrow.get(Calendar.YEAR) - FxAccountConstants.MINIMUM_AGE_TO_CREATE_AN_ACCOUNT;
    Assert.assertFalse("Minimum age fails by a day",
        FxAccountAgeLockoutHelper.passesAgeCheck(birthYear, birthMonthIndex, birthDate));
  }
}
