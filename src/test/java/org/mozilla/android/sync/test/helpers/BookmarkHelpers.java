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

package org.mozilla.android.sync.test.helpers;

import static org.junit.Assert.assertEquals;

import org.mozilla.android.sync.repositories.Utils;
import org.mozilla.android.sync.repositories.domain.BookmarkRecord;

public class BookmarkHelpers {

  private static String parentID = Utils.generateGuid();
  private static String parentName = "main";

  /*
   * Helpers for creating bookmark records of different types
   */
  public static BookmarkRecord createBookmark1() {
    BookmarkRecord record = new BookmarkRecord();
    record.guid = Utils.generateGuid();
    record.title = "Foo!!!";
    record.bookmarkURI = "http://foo.bar.com";
    record.description = "This is a description for foo.bar.com";
    record.loadInSidebar = true;
    record.tags = "[\"tag1\", \"tag2\", \"tag3\"]";
    record.keyword = "fooooozzzzz";
    record.parentID = parentID;
    record.parentName = parentName;
    record.type = "bookmark";
    return record;
  }

  public static BookmarkRecord createBookmark2() {
    BookmarkRecord record = new BookmarkRecord();
    record.guid = Utils.generateGuid();
    record.title = "Bar???";
    record.bookmarkURI = "http://bar.foo.com";
    record.description = "This is a description for Bar???";
    record.loadInSidebar = false;
    record.tags = "[\"tag1\", \"tag2\"]";
    record.keyword = "keywordzzz";
    record.parentID = parentID;
    record.parentName = parentName;
    record.type = "bookmark";
    return record;
  }

  public static BookmarkRecord createMicrosummary() {
    BookmarkRecord record = new BookmarkRecord();
    record.guid = Utils.generateGuid();
    record.generatorURI = "http://generatoruri.com";
    record.staticTitle = "Static Microsummary Title";
    record.title = "Microsummary 1";
    record.bookmarkURI = "www.bmkuri.com";
    record.description = "microsummary description";
    record.loadInSidebar = false;
    record.tags = "[\"tag1\", \"tag2\"]";
    record.keyword = "keywordzzz";
    record.parentID = parentID;
    record.parentName = parentName;
    record.type = "microsummary";
    return record;
  }

  public static BookmarkRecord createQuery() {
    BookmarkRecord record = new BookmarkRecord();
    record.guid = Utils.generateGuid();
    record.folderName = "Query Folder Name";
    record.queryID = "OptionalQueryId";
    record.title = "Query 1";
    record.bookmarkURI = "http://www.query.com";
    record.description = "Query 1 description";
    record.loadInSidebar = true;
    record.tags = "[]";
    record.keyword = "queryKeyword";
    record.parentID = parentID;
    record.parentName = parentName;
    record.type = "query";
    return record;
  }

  public static BookmarkRecord createFolder() {
    // Make this the Menu folder since each DB will
    // have at least this folder
    BookmarkRecord record = new BookmarkRecord();
    record.guid = parentID;
    record.title = parentName;
    // No parent since this is the menu folder
    record.parentID = "";
    record.parentName = "";
    // TODO verify how we want to store these string arrays
    // pretty sure I verified that this is actually how other clients do it, but double check
    record.children = "[\"" + Utils.generateGuid() + "\", \"" + Utils.generateGuid() + "\"]";
    record.type = "folder";
    return record;
  }

  public static BookmarkRecord createLivemark() {
    BookmarkRecord record = new BookmarkRecord();
    record.guid = Utils.generateGuid();
    record.siteURI = "http://site.uri.com";
    record.feedURI = "http://rss.site.uri.com";
    record.title = "Livemark title";
    record.parentID = parentID;
    record.parentName = parentName;
    // TODO verify how we want to store these string arrays
    // pretty sure I verified that this is actually how other clients do it, but double check
    record.children = "[\"" + Utils.generateGuid() + "\", \"" + Utils.generateGuid() + "\"]";
    record.type = "livemark";
    return record;
  }

  public static BookmarkRecord createSeparator() {
    BookmarkRecord record = new BookmarkRecord();
    record.guid = Utils.generateGuid();
    record.pos = "3";
    record.parentID = parentID;
    record.parentName = parentName;
    record.type = "separator";
    return record;
  }

  public static void verifyExpectedRecordReturned(BookmarkRecord expected, BookmarkRecord actual) {
    assertEquals(expected.guid, actual.guid);
    assertEquals(expected.title, actual.title);
    assertEquals(expected.bookmarkURI, actual.bookmarkURI);
    assertEquals(expected.description, actual.description);
  }
}
