/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxa;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import org.mozilla.gecko.fxa.authenticator.AccountPickler;
import org.mozilla.gecko.fxa.authenticator.AndroidFxAccount;
import org.mozilla.gecko.sync.ThreadPool;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

/**
 * Simple public accessors for Firefox account objects.
 */
public class FirefoxAccounts {
  /**
   * Container used to encapsulate an {@link android.accounts.Account Account}. Used to
   * return a value from an asynchronous operation. Note that this container is not
   * synchronized.
   */
  private static class AccountContainer {
    private Account account;

    public AccountContainer() {
      account = null;
    }

    public Account getAccount() { return account; }
    public void setAccount(final Account account) { this.account = account; }
  }

  /**
   * Returns true if a FirefoxAccount exists, false otherwise.
   *
   * @param context Android context.
   * @return true if at least one Firefox account exists.
   */
  public static boolean firefoxAccountsExist(final Context context) {
    return getFirefoxAccounts(context).length > 0;
  }

  /**
   * Return Firefox accounts. If no accounts exist in the AccountManager,
   * one may be created via a pickled FirefoxAccount, if available, and that
   * account will be added to the AccountManager and returned.
   *
   * @param context Android context.
   * @return Firefox account objects.
   */
  public static Account[] getFirefoxAccounts(final Context context) {
    final Account[] accounts =
        AccountManager.get(context).getAccountsByType(FxAccountConstants.ACCOUNT_TYPE);
    if (accounts.length > 0) {
      return accounts;
    }

    final Account pickledAccount = getPickledAccount(context);
    return (pickledAccount != null) ? new Account[] {pickledAccount} : new Account[0];
  }

  private static Account getPickledAccount(final Context context) {
    // To avoid a StrictMode violation for disk access, we call this from a background thread.
    // We do this every time, so the caller doesn't have to care.
    final CountDownLatch latch = new CountDownLatch(1);
    final AccountContainer accountContainer = new AccountContainer();
    ThreadPool.run(new Runnable() {
      @Override
      public void run() {
        try {
          final File file = context.getFileStreamPath(FxAccountConstants.ACCOUNT_PICKLE_FILENAME);
          if (!file.exists()) {
            accountContainer.setAccount(null);
            return;
          }

          // There is a small race window here: if the user creates a new Firefox account
          // between our checks, this could erroneously report that no Firefox accounts
          // exist.
          final AndroidFxAccount fxAccount =
              AccountPickler.unpickle(context, FxAccountConstants.ACCOUNT_PICKLE_FILENAME);
          accountContainer.setAccount(fxAccount.getAndroidAccount());
        } finally {
          latch.countDown();
        }
      }
    });

    try {
      latch.await(); // the background thread.
    } catch (InterruptedException e) {
      throw new IllegalStateException("Thread unexpectedly interrupted", e);
    }

    return accountContainer.getAccount();
  }

  /**
   * @param context Android context.
   * @return the configured Firefox account if one exists, or null otherwise.
   */
  public static Account getFirefoxAccount(final Context context) {
    Account[] accounts = getFirefoxAccounts(context);
    if (accounts.length > 0) {
      return accounts[0];
    }
    return null;
  }
}
