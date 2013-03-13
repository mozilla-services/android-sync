/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.picl.sync.repositories.test;

import org.json.simple.JSONArray;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.picl.PICLAccountConstants;
import org.mozilla.gecko.picl.sync.net.PICLServer0Client;
import org.mozilla.gecko.picl.sync.net.PICLServer0Client.PICLServer0ClientDelegate;
import org.mozilla.gecko.sync.ExtendedJSONObject;

import ch.boye.httpclientandroidlib.HttpResponse;

public class TestPICLServer0Client {
  public static final String TEST_SERVER_URI = PICLAccountConstants.STORAGE_SERVER;
  public static final String TEST_USERID = "testUserID";

  protected PICLServer0Client client;

  @Before
  public void setUp() {
    this.client = new PICLServer0Client(TEST_SERVER_URI, TEST_USERID, "android-tests");
  }

  @Test
  public void testPost() {
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        ExtendedJSONObject json = new ExtendedJSONObject();
        json.put("id", "test1");
        json.put("payload", "test payload");

        JSONArray arr = new JSONArray();
        arr.add(json);

        client.post(arr, new PICLServer0ClientDelegate() {
          @Override
          public void handleSuccess(ExtendedJSONObject extendedJSONObject) {
            WaitHelper.getTestWaiter().performNotify();
            Assert.assertTrue(true);
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

  // a 404 is solved by running this Test again. wah!
  @Test
  public void testGet() {
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.get(new PICLServer0ClientDelegate() {
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
