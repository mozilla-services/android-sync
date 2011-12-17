package org.mozilla.gecko.sync.setup.activities;

import org.mozilla.gecko.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

public class SetupWaitingActivity extends Activity {
  private Context mContext;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.sync_setup_jpake_waiting);
    mContext = this.getApplicationContext();
  }

  public void cancelClickHandler(View target) {
    setResult(RESULT_CANCELED, null);
    finish();
  }


}
