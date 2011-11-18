/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Android Sync Client.
 *
 * The Initial Developer of the Original Code is
 * the Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Jason Voll <jvoll@mozilla.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.android.sync.repositories;

import android.content.Context;
import android.database.Cursor;
import android.provider.Browser;

public class LocalBookmarkSynchronizer {
  /*
   * This class carries out a local sync which is used to
   * keep Mozilla's Bookmark sync database in sync with
   * Android's stock bookmark database.
   */

  // TODO eventually make this a thread or integrate it
  // into some sort of service.

  private Context context;
  private BookmarksDatabaseHelper dbHelper;

  public LocalBookmarkSynchronizer(Context context) {
    this.context = context;
    this.dbHelper = new BookmarksDatabaseHelper(context);
  }

  /*
   * This sync must happen before performing each sync from device
   * to sync server. This pulls all changes from the stock bookmarks
   * db to the local Moz bookmark db.
   */
  public void syncStockToMoz() {

    // Get all bookmarks from Moz table (which will reflect last
    // known state of stock bookmarks - i.e. a snapshot)
    Cursor curMoz = dbHelper.fetchAllBookmarksOrderByAndroidId();
    curMoz.moveToFirst();

    // Get a cursor for the bookmarks in the stock android storage
    Cursor curDroid = context.getContentResolver().query(Browser.BOOKMARKS_URI, null, null, null, Browser.BookmarkColumns._ID);
    curDroid.moveToFirst();

    while (curMoz.isAfterLast() == false) {

      // TODO left off here
    }


    /*
    Uri mBookmarksUri = Browser.BOOKMARKS_URI;

    //TextView view = (TextView) findViewById(R.id.tvHello);
    Cursor cur = managedQuery(mBookmarksUri, null, null, null, null);


    cur.moveToFirst();
    int index = cur.getColumnIndex(Browser.BookmarkColumns.BOOKMARK);
    String[] columns = cur.getColumnNames();
    String test = "";
    for (int i = 0; i < columns.length; i++) {
      test = test + "\t" + columns[i];
    }

    test = test + "\n";

    while (cur.isAfterLast() == false) {
      if (cur.getString(index).equalsIgnoreCase("0")) {
        cur.moveToNext();
        continue;
      }

      for (int i = 0; i < columns.length; i++) {
        try {
          test = test + "\t" + cur.getString(i);
        } catch (Exception e) {
          test = test + "\tEXCEPTION";
        }
      }
      test = test + "\n";
      cur.moveToNext();
    }
    */

  }

}
