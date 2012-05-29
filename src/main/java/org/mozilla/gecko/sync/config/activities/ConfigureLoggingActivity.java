/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.config.activities;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.mozilla.gecko.R;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.log.FileLogManager;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.setup.SyncAccounts.SyncAccountParameters;
import org.mozilla.gecko.sync.syncadapter.SyncAdapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

/**
 * Configure log levels and manage existing log files.
 */
public class ConfigureLoggingActivity extends Activity {
  public final static String LOG_TAG = "ConfigureLoggingAct";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(R.style.SyncTheme);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.sync_configure_logging);
  }

  public interface WithLogLevel {
    public void run(String prefsPath, int logLevel);
  }

  protected void withLogLevel(final WithLogLevel withLogLevel) {
    final Context context = this;
    Account account = (Account) this.getIntent().getExtras().get("account");
    if (account == null) {
      Logger.error(LOG_TAG, "Failed to get account!");
      return;
    }

    SyncAdapter.withSyncAccountParameters(this, AccountManager.get(this), account, new SyncAdapter.WithSyncAccountParameters() {
      @Override
      public void run(SyncAccountParameters syncAccountParameters) {
        String prefsPath = null;
        try {
          prefsPath = Utils.getPrefsPath(syncAccountParameters.username, syncAccountParameters.serverURL);
        } catch (Exception e) {
          Logger.error(LOG_TAG, "Caught exception getting preferences path.", e);
          return;
        }
        if (prefsPath == null) {
          Logger.error(LOG_TAG, "Got null preferences path.");
          return;
        }

        int logLevel = FileLogManager.getLogLevel(context, prefsPath);
        withLogLevel.run(prefsPath, logLevel);
      }
    });
  }

  protected void updateRadioState() {
    withLogLevel(new WithLogLevel() {
      @Override
      public void run(String prefsPath, int logLevel) {
        setLogLevel(logLevel);
      }
    });
  }

  protected void setLogLevel(int logLevel) {
    RadioGroup rg = (RadioGroup) findViewById(R.id.sync_configure_logging_level_radioGroup);
    if (logLevel == 3) {
      rg.check(R.id.sync_configure_logging_level3_radio);
    } else if (logLevel == 2) {
      rg.check(R.id.sync_configure_logging_level2_radio);
    } else if (logLevel == 1) {
      rg.check(R.id.sync_configure_logging_level1_radio);
    } else {
      rg.check(R.id.sync_configure_logging_level0_radio);
    }
  }

  protected int getLogLevel() {
    RadioGroup rg = (RadioGroup) findViewById(R.id.sync_configure_logging_level_radioGroup);
    int id = rg.getCheckedRadioButtonId();
    if (id == R.id.sync_configure_logging_level3_radio) {
      return 3;
    } else if (id == R.id.sync_configure_logging_level2_radio) {
      return 2;
    } else if (id == R.id.sync_configure_logging_level1_radio) {
      return 1;
    } else {
      return 0;
    }
  }

  /**
   * Enable and disable buttons when we have/don't have logs.
   */
  protected void updateButtonStates() {
    boolean hasFileLogs = FileLogManager.hasFileLogs(this);
    Button b;
    for (int id : new int[] { R.id.sync_configure_logging_share_logs_button, R.id.sync_configure_logging_delete_logs_button }) {
      b = (Button) findViewById(id);
      if (b == null) {
        continue;
      }
      b.setEnabled(hasFileLogs);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    updateButtonStates();
    updateRadioState();
  }

  public void okClickHandler(View target) {
    final Context context = this;
    final int newLogLevel = getLogLevel();

    withLogLevel(new WithLogLevel() {
      @Override
      public void run(String prefsPath, int oldLogLevel) {
        Logger.debug(LOG_TAG, "OK clicked with new log level " + newLogLevel +
            " and old log level " + oldLogLevel + ".");
        if (newLogLevel != oldLogLevel && prefsPath != null) {
          Logger.debug(LOG_TAG, "Persisting log level " + newLogLevel + ".");
          FileLogManager.persistLogLevel(context, prefsPath, newLogLevel);
        }

        setResult(RESULT_OK);
        finish();
      }
    });
  }

  public void cancelClickHandler(View target) {
    int newLogLevel = getLogLevel();
    Logger.debug(LOG_TAG, "Cancel clicked with new log level " + newLogLevel + "; not persisting.");
    setResult(RESULT_CANCELED);
    finish();
  }

  public void deleteAllFileLogs() {
    final Context context = this;

    new AlertDialog.Builder(this)
    .setTitle(R.string.sync_configure_logging_delete_all_logs_title)
    .setMessage(R.string.sync_configure_logging_delete_all_logs_confirm)
    .setIcon(android.R.drawable.ic_dialog_alert)
    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        int deleted = FileLogManager.deleteAllFileLogs(context);
        String message = getString(R.string.sync_configure_logging_delete_all_logs_toast, new Integer(deleted));
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        updateButtonStates();
      }
    })
    .setNegativeButton(android.R.string.no, null).show();
  }

  public void deleteFileLogs(List<String> selectedLogs) {
    int deleted = FileLogManager.deleteFileLogs(this, selectedLogs);
    String message = getString(R.string.sync_configure_logging_delete_logs_toast, new Integer(deleted));
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    updateButtonStates();
  }

  public void shareFileLogs(List<String> selectedLogs) {
    String title = getString(R.string.sync_configure_logging_share_logs_title);
    int shared = FileLogManager.shareFileLogs(this, title, selectedLogs);
    String message = getString(R.string.sync_configure_logging_share_logs_toast, new Integer(shared));
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    updateButtonStates();
  }

  public void deleteAllLogsClickHandler(View target) {
    final Context context = this;

    for (int i = 0; i < 3; i++) {
      String name = "FxSync-" + System.currentTimeMillis() + ".txt";
      try {
        FileOutputStream fos = context.openFileOutput(name, Context.MODE_WORLD_READABLE);
        fos.write("text\n".getBytes());
        fos.close();
      } catch (Exception e) {
      }
    }
    updateButtonStates();
  }

  protected interface WithLogs {
    public void withSelected(List<String> logNames);
    public void withAll(List<String> logNames);
  }

  public void shareLogsClickHandler(View target) {
    new WithLogsDialogHelper(this).showLogsDialog(R.string.sync_configure_logging_share_logs_title,
        R.string.sync_configure_logging_share_all_logs_title, new WithLogs() {
      @Override
      public void withSelected(List<String> logNames) {
        shareFileLogs(logNames);
      }

      @Override
      public void withAll(List<String> logNames) {
        shareFileLogs(logNames);
      }
    });
  }

  public void deleteLogsClickHandler(View target) {
    new WithLogsDialogHelper(this).showLogsDialog(R.string.sync_configure_logging_delete_logs_title,
        R.string.sync_configure_logging_delete_all_logs_title, new WithLogs() {
      @Override
      public void withSelected(List<String> logNames) {
        deleteFileLogs(logNames);
      }

      @Override
      public void withAll(List<String> logNames) {
        deleteAllFileLogs();
      }
    });
  }

  protected static class WithLogsDialogHelper
      implements DialogInterface.OnMultiChoiceClickListener,
      DialogInterface.OnClickListener,
      DialogInterface.OnShowListener {
    protected AlertDialog dialog;
    protected WithLogs callback;

    protected final String[]  _options;
    protected final boolean[] _selections;

    protected final Context context;

    public WithLogsDialogHelper(Context context) {
      this.context = context;
      this._options = FileLogManager.getFileLogs(context).toArray(new String[0]);
      this._selections = new boolean[this._options.length];
    }

    protected List<String> getAllLogNames() {
      ArrayList<String> logNames = new ArrayList<String>();
      for (int i = 0; i < _selections.length; i++) {
        logNames.add(_options[i]);
      }
      return logNames;
    }

    protected List<String> getSelectedLogNames() {
      ArrayList<String> logNames = new ArrayList<String>();
      for (int i = 0; i < _selections.length; i++) {
        if (_selections[i]) {
          logNames.add(_options[i]);
        }
      }
      return logNames;
    }

    protected boolean areSelectedLogNames() {
      for (int i = 0; i < _selections.length; i++) {
        if (_selections[i]) {
          return true;
        }
      }
      return false;
    }

    protected void showLogsDialog(int positiveId, int neutralId, WithLogs callback) {
      this.callback = callback;
      dialog = new AlertDialog.Builder(context)
      .setTitle(positiveId)
      .setMultiChoiceItems(_options, _selections, this)
      .setNegativeButton(R.string.sync_button_cancel, null)
      .setNeutralButton(neutralId, this)
      .setPositiveButton(positiveId, this)
      .create();
      dialog.setOnShowListener(this);
      dialog.show();
    }

    @Override
    public void onShow(DialogInterface dialog) {
      ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(areSelectedLogNames());
    }

    public void onClick(DialogInterface dialogInterface, int clicked, boolean selected) {
      dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(areSelectedLogNames());
    }

    public void onClick(DialogInterface dialog, int clicked) {
      switch(clicked) {
      case DialogInterface.BUTTON_NEGATIVE:
        break;
      case DialogInterface.BUTTON_NEUTRAL:
        callback.withAll(getAllLogNames());
        break;
      case DialogInterface.BUTTON_POSITIVE:
        callback.withSelected(getSelectedLogNames());
        break;
      }
    }
  }
}
