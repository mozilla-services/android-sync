/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.healthreport.test;

import android.content.Intent;
import java.util.concurrent.BrokenBarrierException;

import org.mozilla.gecko.background.healthreport.HealthReportBroadcastService;
import org.mozilla.gecko.background.test.helpers.BackgroundServiceTestCase;

public class TestHealthReportBroadcastService
    extends BackgroundServiceTestCase<TestHealthReportBroadcastService.MockHealthReportBroadcastService> {
  public static class MockHealthReportBroadcastService extends HealthReportBroadcastService {
    @Override
    protected void onHandleIntent(Intent intent) {
      super.onHandleIntent(intent);
      try {
        barrier.await();
      } catch (InterruptedException e) {
        fail("Awaiting Service thread should not be interrupted.");
      } catch (BrokenBarrierException e) {
        // This will happen on timeout - do nothing.
      }
    }
  }

  public TestHealthReportBroadcastService() {
    super(MockHealthReportBroadcastService.class);
  }
}
