package org.mozilla.gecko.sync.setup.activities;

import org.mozilla.gecko.R;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.android.ClientsDatabaseAccessor;
import org.mozilla.gecko.sync.repositories.domain.ClientRecord;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListView;

public class SendTabActivity extends Activity {
  public static final String LOG_TAG = "SendTabActivity";
  private ListView listview;
  private ClientRecordArrayAdapter arrayAdapter;
  private String title;
  private String uri;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(R.style.SyncTheme);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.sync_send_tab);

    arrayAdapter = new ClientRecordArrayAdapter(this, R.layout.sync_list_item, getClientArray());

    listview = (ListView) findViewById(R.id.device_list);
    listview.setAdapter(arrayAdapter);
    listview.setItemsCanFocus(true);
    listview.setTextFilterEnabled(true);
    listview.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    enableSend(false);
  }

  public void sendClickHandler(View view) {
    String[] guids = arrayAdapter.getCheckedGUIDs();

    String guidsString = TextUtils.join(", ", guids);
    Logger.debug(LOG_TAG, "Sending to "  + guids.length + " clients with guids " + guidsString + ".");

    Bundle extras = this.getIntent().getExtras();
    uri = extras.getString(Intent.EXTRA_TEXT);
    title = extras.getString(Intent.EXTRA_SUBJECT);
    Logger.pii(LOG_TAG, "Sending title: " + title);
    Logger.pii(LOG_TAG, "Sending uri:   " + uri);
  }

  public void enableSend(boolean shouldEnable) {
    View sendButton = findViewById(R.id.send_button);
    sendButton.setEnabled(shouldEnable);
    sendButton.setClickable(shouldEnable);
  }

  protected ClientRecord[] getClientArray() {
    ClientsDatabaseAccessor db = new ClientsDatabaseAccessor(this.getApplicationContext());

    try {
      return db.fetchAll().values().toArray(new ClientRecord[0]);
    } catch (NullCursorException e) {
      Logger.warn(LOG_TAG, "NullCursorException while populating device array.", e);
      return null;
    } finally {
      db.close();
    }
  }
}
