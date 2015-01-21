/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.fxa;

import java.util.Arrays;
import java.util.Calendar;

import org.mozilla.gecko.fxa.FxAccountConstants;

/**
 * Utility to manage COPPA age verification requirements.
 * <p>
 * A user who fails an age verification check when trying to create an account
 * is denied the ability to make an account for a period of time. We refer to
 * this state as being "locked out".
 * <p>
 * For now we maintain "locked out" state as a static variable. In the future we
 * might need to persist this state across process restarts, so we'll force
 * consumers to create an instance of this class. Then, we can drop in a class
 * backed by shared preferences.
 */
public class FxAccountAgeLockoutHelper {
  private static final String LOG_TAG = FxAccountAgeLockoutHelper.class.getSimpleName();

  protected static long ELAPSED_REALTIME_OF_LAST_FAILED_AGE_CHECK = 0L;

  public static synchronized boolean isLockedOut(long elapsedRealtime) {
    if (ELAPSED_REALTIME_OF_LAST_FAILED_AGE_CHECK == 0L) {
      // We never failed, so we're not locked out.
      return false;
    }

    // Otherwise, find out how long it's been since we last failed.
    long millsecondsSinceLastFailedAgeCheck = elapsedRealtime - ELAPSED_REALTIME_OF_LAST_FAILED_AGE_CHECK;
    boolean isLockedOut = millsecondsSinceLastFailedAgeCheck < FxAccountConstants.MINIMUM_TIME_TO_WAIT_AFTER_AGE_CHECK_FAILED_IN_MILLISECONDS;
    FxAccountUtils.pii(LOG_TAG, "Checking if locked out: it's been " + millsecondsSinceLastFailedAgeCheck + "ms " +
        "since last lockout, so " + (isLockedOut ? "yes." : "no."));
    return isLockedOut;
  }

  public static synchronized void lockOut(long elapsedRealtime) {
      FxAccountUtils.pii(LOG_TAG, "Locking out at time: " + elapsedRealtime);
      ELAPSED_REALTIME_OF_LAST_FAILED_AGE_CHECK = Math.max(elapsedRealtime, ELAPSED_REALTIME_OF_LAST_FAILED_AGE_CHECK);
  }

  /**
   * Return true if the yearText is magic year.
   *
   * @param yearOfBirth
   * @return true if <code>yearOfBirth</code> is the magic year.
   */
  public static boolean isMagicYear(int yearOfBirth) {
    final Calendar cal = Calendar.getInstance();
    final int thisYear = cal.get(Calendar.YEAR);
    return (thisYear - yearOfBirth) == FxAccountConstants.MINIMUM_AGE_TO_CREATE_AN_ACCOUNT;
  }

  /**
   * Return true if the age of somebody born in <code>yearOfBirth/zeroBasedMonthOfBirth/dayOfBirth</code> is
   * definitely old enough to create an account.
   *
   * @param dayOfBirth
   * @param zeroBasedMonthOfBirth
   * @param yearOfBirth
   * @return true if somebody born in <code>yearOfBirth/monthIndex/dayOfBirth</code> is definitely old
   *         enough.
   */
  public static boolean passesAgeCheck(final int dayOfBirth, final int zeroBasedMonthOfBirth, final int yearOfBirth) {
    final Calendar cal = Calendar.getInstance();
    final int thisYear = cal.get(Calendar.YEAR);
    // Get current zero based month index.
    final int thisMonth = cal.get(Calendar.MONTH);
    // Get current one based day of the month.
    final int thisDate = cal.get(Calendar.DAY_OF_MONTH);
    int approximateAge = thisYear - yearOfBirth;

    // Adjust age to account for month and day in case yearOfBirth is magic year.
    if (isMagicYear(yearOfBirth) && ((thisMonth < zeroBasedMonthOfBirth) || ((thisMonth == zeroBasedMonthOfBirth) && (thisDate < dayOfBirth)))) {
      approximateAge--;
    }

    boolean oldEnough = approximateAge >= FxAccountConstants.MINIMUM_AGE_TO_CREATE_AN_ACCOUNT;
    if (FxAccountUtils.LOG_PERSONAL_INFORMATION) {
      FxAccountUtils.pii(LOG_TAG, "Age check " + (oldEnough ? "passes" : "fails") +
          ": age is " + approximateAge + " = " + thisYear + " - " + yearOfBirth);
    }

    return oldEnough;
  }

  /**
   * Custom function for UI use only.
   */
  public static boolean passesAgeCheck(int dayOfBirth, int monthIndex, String yearText, String[] yearItems) {
    if (yearText == null) {
      throw new IllegalArgumentException("yearText must not be null");
    }
    if (yearItems == null) {
      throw new IllegalArgumentException("yearItems must not be null");
    }
    if (!Arrays.asList(yearItems).contains(yearText)) {
      // This should never happen, but let's be careful.
      FxAccountUtils.pii(LOG_TAG, "Failed age check: year text was not found in item list.");
      return false;
    }
    Integer yearOfBirth;
    try {
      yearOfBirth = Integer.valueOf(yearText, 10);
    } catch (NumberFormatException e) {
      // Any non-numbers in the list are ranges (and we say as much to
      // translators in the resource file), so these people pass the age check.
      FxAccountUtils.pii(LOG_TAG, "Passed age check: year text was found in item list but was not a number.");
      return true;
    }

    return passesAgeCheck(dayOfBirth, monthIndex, yearOfBirth);
  }
}
