/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.config.activities;

import java.util.HashSet;
import java.util.Set;

import org.mozilla.gecko.R;
import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.log.Logger;
import org.mozilla.gecko.sync.setup.SyncAccounts.SyncAccountParameters;

import android.accounts.Account;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;

/**
 * Configure which engines to Sync.
 */
public class ConfigureEnginesActivity extends AndroidSyncConfigureActivity
    implements DialogInterface.OnClickListener, OnMultiChoiceClickListener {
  public final static String LOG_TAG = "ConfigureEnginesAct";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  protected String[]  _options = new String[] { "Bookmarks", "History", "Passwords", "Tabs" };
  protected boolean[] _selections = new boolean[_options.length];

  protected Account getAccount() {
    return (Account) this.getIntent().getExtras().get("account");
  }

  // XXX hardcoded -- blah!
  public void setSelections(Set<String> enabled) {
    _selections[0] = enabled.contains("bookmarks");
    _selections[1] = enabled.contains("forms") && enabled.contains("history");
    _selections[2] = enabled.contains("passwords");
    _selections[3] = enabled.contains("tabs");
  }

  @Override
  public void onResume() {
    super.onResume();

    final ConfigureEnginesActivity self = this;
    withAccountAndPreferences(new WithAccountAndPreferences() {
      @Override
      public void run(SyncAccountParameters params, SharedPreferences prefs) {
        Set<String> enabledEngineNames = SyncConfiguration.getEnabledEngineNames(prefs);
        if (enabledEngineNames == null) {
          enabledEngineNames = new HashSet<String>();
        }
        setSelections(enabledEngineNames);

        new AlertDialog.Builder(self)
        .setTitle(R.string.sync_configure_engines_sync_my_title)
        .setMultiChoiceItems(_options, _selections, self)
        .setIcon(R.drawable.sync_ic_launcher)
        .setPositiveButton(android.R.string.ok, self)
        .setNegativeButton(android.R.string.cancel, self).show();
      }
    });
  }

  protected void requestSync() {
    final Account account = getAccount();
    if (account == null) {
      Logger.error(LOG_TAG, "Failed to get account!");
      return;
    }

    final Bundle extras = new Bundle();
    extras.putString("TEST", "test");
    extras.putBoolean("force", true); // XXX not cool for a full sync??

    ContentResolver.requestSync(account, BrowserContract.AUTHORITY, extras);
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (which == DialogInterface.BUTTON_POSITIVE) {
      requestSync();
      setResult(RESULT_OK);
      finish();
    }
    if (which == DialogInterface.BUTTON_NEGATIVE) {
      setResult(RESULT_CANCELED);
      finish();
    }
  }

  @Override
  public void onClick(DialogInterface dialog, int which, boolean isChecked) {
  }
}
