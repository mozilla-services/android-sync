/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.picl.sync.repositories.test;

import java.net.URI;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.picl.sync.repositories.PICLTabsClient;
import org.mozilla.gecko.picl.sync.repositories.PICLTabsClient.PICLTabsDelegate;
import org.mozilla.gecko.sync.ExtendedJSONObject;

import ch.boye.httpclientandroidlib.HttpResponse;

public class TestPICLTabsClient {
  public static final URI TEST_SERVER_URI = URI.create("http://23.20.0.206");
  public static final String TEST_USERID = "testUserID";

  protected PICLTabsClient tabsClient;

  @Before
  public void setUp() {
    this.tabsClient = new PICLTabsClient(TEST_SERVER_URI, TEST_USERID);
  }

  @Test
  public void test() {
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        tabsClient.getAllTabs(new PICLTabsDelegate() {
          @Override
          public void handleSuccess(ExtendedJSONObject extendedJSONObject) {
            WaitHelper.getTestWaiter().performNotify();
          }

          @Override
          public void handleFailure(HttpResponse response, Exception e) {
            WaitHelper.getTestWaiter().performNotify(e);
          }

          @Override
          public void handleError(Exception e) {
            WaitHelper.getTestWaiter().performNotify(e);
          }
        });
      }
    });
  }
}
