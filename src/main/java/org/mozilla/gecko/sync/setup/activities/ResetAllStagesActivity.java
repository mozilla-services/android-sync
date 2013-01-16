/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.setup.activities;

import org.json.simple.JSONArray;
import org.mozilla.gecko.sync.CommandProcessor.Command;

public class ResetAllStagesActivity extends SendCommandActivity {

  @Override
  protected Command getCommand() {
    final JSONArray args = new JSONArray();
    final Command resetAllCommand = new Command("resetAll", args);

    return resetAllCommand;
  }

  @Override
  protected String[] getStagesToSync() {
    return new String[] { "clients" };
  }
}
