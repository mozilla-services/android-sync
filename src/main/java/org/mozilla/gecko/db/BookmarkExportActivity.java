/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.db;

import java.io.File;
import java.io.FileOutputStream;

import org.mozilla.gecko.background.common.log.Logger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

public class BookmarkExportActivity extends Activity implements
    DialogInterface.OnClickListener, DialogInterface.OnMultiChoiceClickListener {
  public final static String LOG_TAG = "BookmarkExportAct";

  protected final String[] _options = new String[2];
  protected final boolean[] _selections = new boolean[2];

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onResume() {
    super.onResume();

    _options[0] = "Mobile";
    _options[1] = "Desktop";
    _selections[0] = true;
    _selections[1] = false;

    AlertDialog dialog = new AlertDialog.Builder(this)
        .setTitle("Export bookmarks")
        .setMultiChoiceItems(_options, _selections, this)
        .setPositiveButton("Export", this)
        .setNegativeButton(android.R.string.cancel, this).create();

    dialog.setOnDismissListener(new OnDismissListener() {
      @Override
      public void onDismiss(DialogInterface dialog) {
        finish();
      }
    });

    dialog.show();
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (which == DialogInterface.BUTTON_POSITIVE) {
      setResult(RESULT_OK);
      final String s;
      if (_selections[0] && _selections[1]) {
        s = "Exported both";
      } else if (_selections[0]) {
        s = "Exported mobile bookmarks";
      } else if (_selections[1]) {
        s = "Exported desktop bookmarks";
      } else {
        s = "Exported nothing!";
      }

      Toast.makeText(this, s, Toast.LENGTH_SHORT).show();

      final Intent shareIntent = new Intent();
      shareIntent.setAction(Intent.ACTION_SEND);


      try {
        // Use GeckoApp.getTemptDirectory.
        final File dir = getExternalFilesDir("temp");
        final File imageFile = File.createTempFile("image", ".json", dir);
        final FileOutputStream os = new FileOutputStream(imageFile);
        byte[] buf = "test".getBytes("UTF-8");
        os.write(buf);
        os.close();

        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(imageFile));
      } catch (Exception e) {
        Logger.error("XXX", "Failure", e);
      }


      shareIntent.setType("text/plain");
      startActivity(Intent.createChooser(shareIntent, "title"));
      // final Intent intent = new Intent()
    } else {
      setResult(RESULT_CANCELED);
    }
    finish();
  }

  @Override
  public void onClick(DialogInterface dialog, int which, boolean isChecked) {
    // Display multi-selection clicks in UI.
    _selections[which] = isChecked;
    ListView selectionsList = ((AlertDialog) dialog).getListView();
    selectionsList.setItemChecked(which, isChecked);
  }
}
