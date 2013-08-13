/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.test.helpers;

import android.app.Service;
import android.content.Intent;
import android.test.ServiceTestCase;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

/**
 * An abstract test class for testing background services. Since we have to wait for background
 * services to finish before asserting the changed state, this class provides much of the
 * functionality to do this. Extending classes need still need to implement some of the components -
 * see {@link TestHealthReportBroadcastService} for an example.
 */
public abstract class BackgroundServiceTestCase<T extends Service> extends ServiceTestCase<T> {
  protected static final int DEFAULT_WAIT_TIME = 10;
  protected static final TimeUnit DEFAULT_WAIT_UNIT = TimeUnit.SECONDS;

  private final Class<T> mServiceClass;

  protected static CyclicBarrier barrier;
  protected Intent intent;

  public BackgroundServiceTestCase(Class<T> serviceClass) {
    super(serviceClass);
    mServiceClass = serviceClass;
  }

  @Override
  public void setUp() throws Exception {
    barrier = new CyclicBarrier(2);
    intent = new Intent(getContext(), mServiceClass);
  }

  @Override
  public void tearDown() throws Exception {
    barrier = null;
    intent = null;
  }

  protected void awaitOrFail() {
    try {
      barrier.await(DEFAULT_WAIT_TIME, DEFAULT_WAIT_UNIT);
    } catch (TimeoutException e) {
      fail("Background Service did not complete within " + DEFAULT_WAIT_TIME + " " +
          DEFAULT_WAIT_UNIT.toString().toLowerCase() + ".");
    } catch (InterruptedException e) {
      fail("Test runner thread should not be interrupted.");
    } catch (BrokenBarrierException e) {
      fail("Background services should not timeout or be interrupted");
    }
  }
}
