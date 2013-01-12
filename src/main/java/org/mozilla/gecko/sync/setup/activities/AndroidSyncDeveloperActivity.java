/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.setup.activities;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.gecko.sync.Logger;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class AndroidSyncDeveloperActivity extends ListActivity {
  public static final String LOG_TAG = AndroidSyncDeveloperActivity.class.getSimpleName();

  protected final List<String> options = new ArrayList<String>();
  protected final List<Intent> intents = new ArrayList<Intent>();

  protected ArrayAdapter<String> adapter;

  protected void prepareOptionsAndIntents() {
    options.clear();
    intents.clear();

    options.add("Create test sync accounts");
    intents.add(new Intent(this, CreateTestAccountsActivity.class));

    options.add("Sync now");
    intents.add(new Intent(this, SyncNowActivity.class));

    options.add("Reset all stages");
    intents.add(new Intent(this, ResetAllStagesActivity.class));

    options.add("Wipe all stages");
    intents.add(new Intent(this, WipeAllStagesActivity.class));

    options.add("Send mozilla.com tab");
    Intent intent = new Intent(this, SendTabActivity.class);
    intent.putExtra(Intent.EXTRA_TEXT, "http://mozilla.com");
    intent.putExtra(Intent.EXTRA_SUBJECT, "mozilla.com");
    intents.add(intent);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Logger.debug(LOG_TAG, "onCreate");
    super.onCreate(savedInstanceState);

    prepareOptionsAndIntents();

    adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, options);
    setListAdapter(adapter);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    Logger.debug(LOG_TAG, "onListItemClick(" + position + ")");

    String option = options.get(position);
    Intent intent = intents.get(position);

    Logger.info(LOG_TAG, option);

    startActivity(intent);
  }
}
