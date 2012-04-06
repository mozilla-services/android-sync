package org.mozilla.gecko.sync.setup.activities;

import org.mozilla.gecko.R;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.android.ClientsDatabaseAccessor;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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

    arrayAdapter = new ClientRecordArrayAdapter(this, R.layout.sync_list_item, getDeviceMap());

    listview = (ListView) findViewById(R.id.device_list);
    listview.setAdapter(arrayAdapter);
    listview.setItemsCanFocus(true);
    listview.setTextFilterEnabled(true);
    listview.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    enableSend(false);
  }

  public void sendClickHandler(View view) {
    System.out.println("Send was clicked");
    String[] guids = arrayAdapter.getCheckedGUIDs();

    for (int i = 0; i < guids.length; i++) {
      System.out.println("GUID: " + guids[i]);
    }
    Bundle extras = this.getIntent().getExtras();
    uri = extras.getString(Intent.EXTRA_TEXT);
    title = extras.getString(Intent.EXTRA_SUBJECT);
    System.out.println("URI: " + uri);
    System.out.println("TITLE: " + title);
  }

  public void enableSend(boolean shouldEnable) {
    View sendButton = findViewById(R.id.send_button);
    sendButton.setEnabled(shouldEnable);
    sendButton.setClickable(shouldEnable);
  }

  protected Object[] getDeviceMap() {
    ClientsDatabaseAccessor db = new ClientsDatabaseAccessor(this.getApplicationContext());

    try {
      return db.fetchAll().values().toArray();
    } catch (NullCursorException e) {
      Logger.debug(LOG_TAG, "NullCursorException while populating device list.");
      return null;
    } finally {
      db.close();
    }
  }
}
