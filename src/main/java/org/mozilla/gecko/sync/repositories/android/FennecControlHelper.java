/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories.android;

import org.mozilla.gecko.db.BrowserContract.Control;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.repositories.NoContentProviderException;

import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * This class provides an interface to Fenec's control content provider, which
 * exposes some of Fennec's internal state.
 */
public class FennecControlHelper {
  public static String LOG_TAG = "FennecControlHelper";

  protected static Uri FENNEC_CONTROL_URI = Control.CONTENT_URI;

  protected ContentProviderClient providerClient;
  protected final RepoUtils.QueryHelper queryHelper;

  public FennecControlHelper(Context context)
      throws NoContentProviderException {
    providerClient = acquireContentProvider(context);
    queryHelper = new RepoUtils.QueryHelper(context, FENNEC_CONTROL_URI, LOG_TAG);
  }

  /**
   * Acquire the content provider client.
   * <p>
   * The caller is responsible for releasing the client.
   *
   * @param context The application context.
   * @return The <code>ContentProviderClient</code>.
   * @throws NoContentProviderException
   */
  public static ContentProviderClient acquireContentProvider(final Context context)
      throws NoContentProviderException {
    Uri uri = FENNEC_CONTROL_URI;
    ContentProviderClient client = context.getContentResolver().acquireContentProviderClient(uri);
    if (client == null) {
      throw new NoContentProviderException(uri);
    }
    return client;
  }

  public void releaseProviders() {
    try {
      if (providerClient != null) {
        providerClient.release();
      }
    } catch (Exception e) {
    }
    providerClient = null;
  }

  // Only used for testing.
  public ContentProviderClient getFormsProvider() {
    return providerClient;
  }

  protected static String[] HISTORY_MIGRATION_COLUMNS = new String[] { Control.ENSURE_HISTORY_MIGRATED };
  protected static String[] BOOKMARKS_MIGRATION_COLUMNS = new String[] { Control.ENSURE_BOOKMARKS_MIGRATED };

  protected boolean isColumnMigrated(String column) {
    String[] columns = new String[] { column };
    Cursor cursor = null;
    try {
      cursor = queryHelper.safeQuery(providerClient, ".isColumnMigrated(" + column + ")", columns, null, null, null);
      if (!cursor.moveToFirst()) {
        return false;
      }
      int value = RepoUtils.getIntFromCursor(cursor, column);
      return value > 0;
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Caught exception checking if Fennec has migrated column " + column + ".", e);
      return false;
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  protected static boolean isColumnMigrated(Context context, String column) {
    if (context == null) {
      return false;
    }
    FennecControlHelper control = null;
    try {
      control = new FennecControlHelper(context);
      return control.isColumnMigrated(column);
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Caught exception checking if Fennec has migrated column " + column + ".", e);
      return false;
    } finally {
      if (control != null) {
        control.releaseProviders();
      }
    }
  }

  public static boolean isHistoryMigrated(Context context) {
    return isColumnMigrated(context, Control.ENSURE_HISTORY_MIGRATED);
  }

  public static boolean areBookmarksMigrated(Context context) {
    return isColumnMigrated(context, Control.ENSURE_BOOKMARKS_MIGRATED);
  }
}
