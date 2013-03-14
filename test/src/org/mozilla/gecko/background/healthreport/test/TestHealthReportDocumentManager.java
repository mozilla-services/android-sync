/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.healthreport.test;

import org.mozilla.android.sync.test.AndroidSyncTestCase;
import org.mozilla.gecko.background.healthreport.HealthReportDocumentManager;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.mock.MockContext;

public class TestHealthReportDocumentManager extends AndroidSyncTestCase {
  public static final String GECKO_REPORT = "{" +
      "   \"data\": {" +
      "       \"last\": {" +
      "           \"geckoProvider1\": {}," +
      "       }," +
      "       \"days\": {" +
      "           \"date1\": {" +
      "               \"geckoProvider2\": {}," +
      "           }," +
      "           \"date2\": {" +
      "               \"geckoProvider2\": {}," +
      "           }," +
      "           \"date4\": {" +
      "               \"geckoProvider2\": {}," +
      "           }," +
      "       }," +
      "   }" +
      "}";
  public static final String ANDROID_REPORT = "{" +
      "    \"data\": {" +
      "        \"last\" {" +
      "            \"androidProvider1\": {}," +
      "        }," +
      "        \"days\" {" +
      "            \"date1\": {" +
      "                \"androidProvider2\": {}," +
      "            }," +
      "            \"date3\": {" +
      "                \"androidProvider2\": {}," +
      "            }," +
      "            \"date4\": {" +
      "                \"androidProvider2\": {}," +
      "            }," +
      "        }," +
      "    }" +
      "}";
  public static final String MERGED_REPORT = "{" +
      "   \"data\": {" +
      "       \"last\": {" +
      "           \"geckoProvider1\": {}," +
      "           \"androidProvider1\": {}," +
      "       }," +
      "       \"days\": {" +
      "           \"date1\": {" +
      "               \"geckoProvider2\": {}," +
      "               \"androidProvider2\": {}," +
      "           }," +
      "           \"date2\": {" +
      "               \"geckoProvider2\": {}," +
      "           }," +
      "           \"date3\": {" +
      "                \"androidProvider2\": {}," +
      "            }," +
      "           \"date4\": {" +
      "               \"geckoProvider2\": {}," +
      "               \"androidProvider2\": {}," +
      "           }," +
      "       }," +
      "    }" +
      "}";

  class MockHealthReportContext extends MockContext {
    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
      return null;
    }
  }

  public void testDocumentMerge() {
    Context mockContext = new MockHealthReportContext();
    // Passing in null params because we don't use them.
    HealthReportDocumentManager hrdm = new HealthReportDocumentManager(mockContext, null);
    ExtendedJSONObject geckoJSON = null;
    ExtendedJSONObject androidJSON = null;
    ExtendedJSONObject mergedJSON = null;
    try {
      geckoJSON = ExtendedJSONObject.parseJSONObject(GECKO_REPORT);
      androidJSON = ExtendedJSONObject.parseJSONObject(ANDROID_REPORT);
    } catch (Exception e) {
      fail("Unexpected JSON parse exception: " + e.getMessage());
    }

    try {
      mergedJSON = hrdm.mergeAndroidToGeckoReport(androidJSON, geckoJSON);
    } catch (NonObjectJSONException e) {
      fail("Incorrect FHR JSON data format.");
    }
    try {
      ExtendedJSONObject expectedDocument = new ExtendedJSONObject(MERGED_REPORT);
      // Compare JSONObjects.
      assertEquals(expectedDocument.object, mergedJSON.object);
    } catch (Exception e) {
      fail("Unexpected JSON parse exception: " + e.getMessage());
    }
  }
}