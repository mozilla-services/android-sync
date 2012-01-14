/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.delegates.GlobalSessionCallback;
import org.mozilla.gecko.sync.repositories.RecordFactory;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;
import org.mozilla.gecko.sync.net.SyncStorageResponse;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.ProtocolVersion;
import ch.boye.httpclientandroidlib.message.BasicHttpResponse;
import ch.boye.httpclientandroidlib.message.BasicStatusLine;

import android.content.Context;
import android.content.SharedPreferences;
import org.mozilla.android.sync.test.helpers.MockSharedPreferences;

/**
 * A callback for use with a GlobalSession that ensures that
 * handleSuccess is called after the final stage is completed.
 */
public class MockGlobalSessionCallback implements GlobalSessionCallback {
  int stageCounter = Stage.values().length - 1; // Exclude starting state.
  public boolean calledSuccess = false;
    
  public void handleError(GlobalSession globalSession, Exception ex) {
    ex.printStackTrace();
    fail("No error should occur.");
  }

  public void handleSuccess(GlobalSession globalSession) {
    assertEquals(0, stageCounter);
    calledSuccess = true;
  }

  public void handleStageCompleted(Stage currentState,
				   GlobalSession globalSession) {
    stageCounter--;
  }

  @Override
  public void requestBackoff(long backoff) {
    fail("Not expecting backoff.");
  }

  @Override
  public void handleAborted(GlobalSession globalSession, String reason) {
    fail("Not expecting abort.");
  }

  @Override
  public boolean shouldBackOff() {
    return false;
  }
}
