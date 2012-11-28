/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.setup.activities;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.NonArrayJSONException;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.setup.SyncAccounts;
import org.mozilla.gecko.sync.setup.SyncAccounts.SyncAccountParameters;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Activity that creates Android Sync accounts for testing.
 * <p>
 * Reads a JSON array of test accounts and instantiates them. Each test account
 * is in the format of <code>SyncAccounts.SyncAccountParameters.asJSON</code>.
 */
public class CreateTestAccountsActivity extends SyncActivity {
  public static final String LOG_TAG = CreateTestAccountsActivity.class.getSimpleName();

  public static final String TEST_SYNC_ACCOUNTS_JSON = "test.sync.accounts.json";

  public static class CreateTestSyncAccountException extends Exception {
    private static final long serialVersionUID = 4029894835103479058L;

    public CreateTestSyncAccountException(String detailMessage) {
      super(detailMessage);
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onResume() {
    Logger.info(LOG_TAG, "onResume");
    super.onResume();

    try {
      JSONArray testUsers = loadTestUsers();

      if (testUsers.isEmpty()) {
        throw new CreateTestSyncAccountException("No test users to create!");
      }

      Logger.info(LOG_TAG, "Got " + testUsers.size() + " test users.");

      ArrayList<String> names = new ArrayList<String>();

      for (Object testUser : testUsers) {
        String name = createTestUser((JSONObject) testUser);
        if (name == null) {
          throw new CreateTestSyncAccountException("Couldn't make account!");
        }

        names.add(name);
      }

      setResult(RESULT_OK);

      String message = null;
      if (names.isEmpty()) {
        message = "No accounts created.";
      } else if (names.size() == 1) {
        message = "Created account " + Utils.toCommaSeparatedString(names) + ".";
      } else {
        message = "Created accounts " + Utils.toCommaSeparatedString(names) + ".";
      }

      Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    } catch (CreateTestSyncAccountException e) {
      setResult(RESULT_CANCELED);
      Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
    } catch (Exception e) {
      setResult(RESULT_CANCELED);
      Toast.makeText(getApplicationContext(), "Got exception: " + e.toString(), Toast.LENGTH_LONG).show();
    } finally {
      finish();
    }
  }

  /**
   * Helper to create Android Account from single test account datum.
   *
   * @param testUser test account data.
   * @return Android Account name, or null on failure.
   * @throws Exception
   */
  protected String createTestUser(JSONObject testUser) throws Exception {
    ExtendedJSONObject o = new ExtendedJSONObject(testUser);

    SyncAccountParameters syncAccountParameters = new SyncAccounts.SyncAccountParameters(this, null, o);
    Logger.debug(LOG_TAG, "Creating " + syncAccountParameters.username + ".");

    // This is inefficient, but not worth improving.
    Account[] accounts = AccountManager.get(this).getAccounts();
    for (Account account : accounts) {
      if (syncAccountParameters.username.equals(account.name)) {
        // White lie: it wasn't created, but it exists.
        return syncAccountParameters.username;
      }
    }

    Account createdAccount = SyncAccounts.createSyncAccount(syncAccountParameters);

    if (createdAccount == null) {
      return null;
    }

    return createdAccount.name;
  }

  /**
   * Helper to load test accounts from JSON file.
   *
   * @return array of test accounts.
   * @throws IOException
   * @throws ParseException
   * @throws NonArrayJSONException
   */
  private JSONArray loadTestUsers() throws IOException, ParseException, NonArrayJSONException {
    AssetManager assetManager = getAssets();
    InputStream is = assetManager.open(TEST_SYNC_ACCOUNTS_JSON);
    try {
      return ExtendedJSONObject.parseJSONArray(new InputStreamReader(is));
    } finally {
      is.close();
    }
  }
}
