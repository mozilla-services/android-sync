/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.config.activities;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mozilla.gecko.R;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.setup.Constants;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Provides a user-facing interface for selecting engines to sync. This activity can be launched
 * from the Sync Settings preferences screen, and will save the selected engines to a pref.
 * 
 * On launch, it loads from either the saved pref of selected engines (if it exists) or from
 * SyncConfiguration. During a sync, this pref will be read and cleared.
 * 
 */
public class ConfigureEnginesActivity extends AndroidSyncConfigureActivity
    implements DialogInterface.OnClickListener, DialogInterface.OnMultiChoiceClickListener {
  public final static String LOG_TAG = "ConfigureEnginesAct";
  private ListView selectionsList;

  private String[] _options;
  protected boolean[] _selections;
  private boolean[] _origSelections;

  @Override
  public void onResume() {
    super.onResume();
    final ConfigureEnginesActivity self = this;
    Logger.warn(LOG_TAG, "bookmarks");
    Logger.warn(LOG_TAG, getString(R.string.sync_configure_engines_title_bookmarks));
    _options = new String[] {
        getString(R.string.sync_configure_engines_title_bookmarks),
        getString(R.string.sync_configure_engines_title_passwords),
        getString(R.string.sync_configure_engines_title_history),
        getString(R.string.sync_configure_engines_title_tabs) };
    // Display engine configure UI.
    _selections = new boolean[_options.length];
    _origSelections = new boolean[_options.length];
    fetchPrefsAndConsume(new PrefsConsumer() {

      @Override
      public void run(SharedPreferences prefs) {
        setSelections(getEnginesToSelect(prefs));

        new AlertDialog.Builder(self)
            .setTitle(R.string.sync_configure_engines_sync_my_title)
            .setMultiChoiceItems(_options, _selections, self)
            .setIcon(R.drawable.icon)
            .setPositiveButton(android.R.string.ok, self)
            .setNegativeButton(android.R.string.cancel, self).show();
      }
    });
  }

  /**
   * Fetches the engine names that should be displayed as selected for syncing.
   * Check first for the selection pref set by this activity, then the set of
   * enabled engines from SyncConfiguration, and finally use the set of valid
   * engine names for Android Sync.
   *
   * @param syncPrefs
   * @return Set<String> of engine names to display as selected. Should never be
   *         null.
   */
  private Set<String> getEnginesToSelect(SharedPreferences syncPrefs) {
    Set<String> engines;
    // Check engine SharedPrefs first.
    SharedPreferences engineConfigPrefs = mContext.getSharedPreferences(Constants.PREFS_ENGINE_SELECTION, Utils.SHARED_PREFERENCES_MODE);
    @SuppressWarnings("unchecked")
    // We only add booleans to this SharedPreference.
    Map<String, Boolean> engineMap = (Map<String, Boolean>) engineConfigPrefs.getAll();
    if (!engineMap.isEmpty()) {
      engines = new HashSet<String>();
      for (Entry<String, Boolean> pair : engineMap.entrySet()) {
        if (pair.getValue()) {
          engines.add(pair.getKey());
        }
      }
      return engines;
    }
    Logger.warn(LOG_TAG, "No previous engine prefs");
    // No previously stored engine prefs. Fetch from config.

    engines = SyncConfiguration.getEnabledEngineNames(syncPrefs);
    if (engines == null) {
      engines = SyncConfiguration.validEngineNames();
    }
    return engines;
  }

  // Hardcoded engines.
  public void setSelections(Set<String> selected) {
    _selections[0] = selected.contains("bookmarks");
    // Omit Forms, because there is no way to enable/disable Forms in desktop UI.
    _selections[1] = selected.contains("passwords");
    _selections[2] = selected.contains("history");
    _selections[3] = selected.contains("tabs");

    // Cache original selections for comparing changes.
    System.arraycopy(_selections, 0, _origSelections, 0, _selections.length);
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
   * Persists selected engines to SharedPreferences if changed.
   * @return true if changed, false otherwise.
   */
  private void saveSelections() {
    // Persist to SharedPreferences.
    SharedPreferences prefs = mContext.getSharedPreferences(Constants.PREFS_ENGINE_SELECTION, Utils.SHARED_PREFERENCES_MODE);
    Editor enginePrefsEditor = prefs.edit();
    for (int i = 0; i < _selections.length; i++) {
      if (_selections[i] != _origSelections[i]) {
        enginePrefsEditor.putBoolean(_options[i].toLowerCase(), _selections[i]);
      }
    }
    enginePrefsEditor.commit();
  }
}
