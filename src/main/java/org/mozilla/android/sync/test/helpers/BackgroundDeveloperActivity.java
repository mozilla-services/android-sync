/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import org.mozilla.gecko.background.common.log.Logger;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

public class BackgroundDeveloperActivity extends Activity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onResume() {
    super.onResume();

    String message = this.getPackageName() + " started.";
    Logger.info(Logger.DEFAULT_LOG_TAG, message);
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

    finish();
  }
}
