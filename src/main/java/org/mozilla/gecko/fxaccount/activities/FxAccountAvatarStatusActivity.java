/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.fxaccount.activities;

import java.io.InputStream;

import org.mozilla.gecko.R;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Logger;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class FxAccountAvatarStatusActivity extends Activity {
  private static final String LOG_TAG = FxAccountAvatarStatusActivity.class.getSimpleName();

  public static final int REQUEST_CODE_AVATAR_IMAGE = 5;

  protected ImageButton avatarStatusImageButton;
  protected Bitmap bitmap;

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCreate(Bundle icicle) {
    Logger.info(LOG_TAG, "onCreate(" + icicle + ")");
    super.onCreate(icicle);
    setContentView(R.layout.fxa_avatar_status);

    avatarStatusImageButton = (ImageButton) findViewById(R.id.avatar_status_image);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onResume() {
    super.onResume();

    Intent intent = this.getIntent();
    Logger.info(LOG_TAG, "onResume(" + intent + ")");

    String resultString = intent.getStringExtra(FxAccountSetupActivity.PARAM_RESULT);
    ExtendedJSONObject result;
    try {
      result = new ExtendedJSONObject(resultString);
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Got exception!", e);
      return;
    }

    EditText nameEdit = (EditText) findViewById(R.id.avatar_status_name);
    nameEdit.setText(result.getString("name"));

    TextView updateEdit = (TextView) findViewById(R.id.avatar_status_update);
    updateEdit.setText(result.getString("image"));
  }

  public void onAvatarStatusImage(View View) {
    Intent intent = new Intent();
    intent.setType("image/*");
    intent.setAction(Intent.ACTION_GET_CONTENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    startActivityForResult(intent, REQUEST_CODE_AVATAR_IMAGE);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (!(requestCode == REQUEST_CODE_AVATAR_IMAGE && resultCode == Activity.RESULT_OK)) {
      super.onActivityResult(requestCode, resultCode, data);
      return;
    }

    try {
      // We need to recyle unused bitmaps
      if (bitmap != null) {
        bitmap.recycle();
      }

      InputStream stream = getContentResolver().openInputStream(data.getData());
      bitmap = BitmapFactory.decodeStream(stream);
      stream.close();

      avatarStatusImageButton.setImageBitmap(bitmap);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
