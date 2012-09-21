/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.config.activities;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mozilla.gecko.R;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.SyncConfiguration;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Provides a user-facing interface for selecting engines to sync. This activity
 * can be launched from the Sync Settings preferences screen, and will save the
 * selected engines to the
 * <code>SyncConfiguration.PREF_USER_SELECTED_ENGINES_TO_SYNC</code> pref.
 *
 * On launch, it displays the engines stored in the saved pref if it exists, or
 * <code>SyncConfiguration.enabledEngineNames</code> if it doesn't, defaulting
 * to <code>SyncConfiguration.validEngineNames()</code> if neither exists.
 *
 * During a sync, the
 * <code>SyncConfiguration.PREF_USER_SELECTED_ENGINES_TO_SYNC</code> pref will
 * be cleared after the engine changes are applied to meta/global.
 */
public class ConfigureEnginesActivity extends AndroidSyncConfigureActivity
    implements DialogInterface.OnClickListener, DialogInterface.OnMultiChoiceClickListener {
  public final static String LOG_TAG = "ConfigureEnginesAct";
  private ListView selectionsList;

  // Collections names corresponding to displayed (localized) engine options.
  final String[] _collectionsNames = new String[] {
    "bookmarks",
    "passwords",
    "history",
    "tabs"
  };

  // Engine names localized for display in Sync Settings.
  private String[] _options;
  // Selection state of engines corresponding to _options array.
  final boolean[] _selections = new boolean[_collectionsNames.length];
  private SharedPreferences accountPrefs;

  @Override
  public void onResume() {
    super.onResume();
    final ConfigureEnginesActivity self = this;
    _options = new String[] {
        getString(R.string.sync_configure_engines_title_bookmarks),
        getString(R.string.sync_configure_engines_title_passwords),
        getString(R.string.sync_configure_engines_title_history),
        getString(R.string.sync_configure_engines_title_tabs) };

    // Display engine configure UI.
    fetchPrefsAndConsume(new PrefsConsumer() {
      @Override
      public void run(SharedPreferences prefs) {
        self.accountPrefs = prefs;
        setSelectionsInArray(getEnginesToSelect(prefs), _selections);

        new AlertDialog.Builder(self)
            .setTitle(R.string.sync_configure_engines_sync_my_title)
            .setMultiChoiceItems(_options, _selections, self)
            .setIcon(R.drawable.icon)
            .setPositiveButton(android.R.string.ok, self)
            .setNegativeButton(android.R.string.cancel, self).show();
      }
    });
  }

  private Set<String> getEnginesFromPrefs(SharedPreferences syncPrefs) {
    Set<String> engines = SyncConfiguration.getEnabledEngineNames(syncPrefs);
    if (engines == null) {
      engines = SyncConfiguration.validEngineNames();
    }
    return engines;
  }

  /**
   * Fetches the engine names that should be displayed as selected for syncing.
   * Check first for selected engines set by this activity, then the enabled
   * engines, and finally default to the set of valid engine names for Android
   * Sync if neither exists.
   *
   * @param syncPrefs
   *          <code>SharedPreferences</code> of Account being modified.
   * @return Set<String> of engine names to display as selected. Should never be
   *         null.
   */
  private Set<String> getEnginesToSelect(SharedPreferences syncPrefs) {
    Set<String> engines = getEnginesFromPrefs(syncPrefs);
    Map<String, Boolean> engineSelections = SyncConfiguration.getEnginesToSelect(syncPrefs);
    if (engineSelections != null) {
      for (Entry<String, Boolean> pair : engineSelections.entrySet()) {
        if (pair.getValue()) {
          engines.add(pair.getKey());
        } else {
          engines.remove(pair.getKey());
        }
      }
    }
    return engines;
  }

  public void setSelectionsInArray(Set<String> selected, boolean[] array) {
    for (int i = 0; i < _collectionsNames.length; i++) {
      array[i] = selected.contains(_collectionsNames[i]);
    }
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (which == DialogInterface.BUTTON_POSITIVE) {
      Logger.debug(LOG_TAG, "Saving selected engines.");
      saveSelections();
      setResult(RESULT_OK);
      Toast.makeText(this, R.string.sync_notification_savedprefs, Toast.LENGTH_SHORT).show();
    } else {
      setResult(RESULT_CANCELED);
    }
    finish();
  }

  @Override
  public void onClick(DialogInterface dialog, int which, boolean isChecked) {
    // Display multi-selection clicks in UI.
    _selections[which] = isChecked;
    if (selectionsList == null) {
      selectionsList = ((AlertDialog) dialog).getListView();
    }
    selectionsList.setItemChecked(which, isChecked);
  }

  /**
   * Persists engine selection state to SharedPreferences if it has changed.
   *
   * @return true if changed, false otherwise.
   */
  private void saveSelections() {
    boolean[] origSelections = new boolean[_options.length];
    setSelectionsInArray(getEnginesFromPrefs(accountPrefs), origSelections);
    Map<String, Boolean> engineSelections = new HashMap<String, Boolean>();
    for (int i = 0; i < _selections.length; i++) {
      if (_selections[i] != origSelections[i]) {
        engineSelections.put(_collectionsNames[i], _selections[i]);
        // Special case for forms and history, because forms depends on history.
        if (getString(R.string.sync_configure_engines_title_history).equals(_options[i])) {
          engineSelections.put("forms", _selections[i]);
        }
      }
    }
    // No GlobalSession.config, so store directly to prefs.
    SyncConfiguration.storeSelectedEnginesToPrefs(accountPrefs, engineSelections);
  }
}
