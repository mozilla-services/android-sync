/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.repositories.domain.Record;
import org.mozilla.android.sync.test.helpers.BookmarkHelpers;

public class AndroidBookmarksRepoTest extends AndroidBookmarksTestBase {
  
  public void testFetchAll() {
    Record[] expected = new Record[2];
    expected[0] = BookmarkHelpers.createBookmark1();
    expected[1] = BookmarkHelpers.createBookmark2();
    basicFetchAllTest(expected);
  }

  /*
   * Tests for fetching GUIDs since a timestamp.
   */
  public void testGuidsSinceReturnMultipleRecords() {
    BookmarkRecord record0 = BookmarkHelpers.createLivemark();
    BookmarkRecord record1 = BookmarkHelpers.createMicrosummary();
    guidsSinceReturnMultipleRecords(record0, record1);
  }
  
  public void testGuidsSinceReturnNoRecords() {
    guidsSinceReturnNoRecords(BookmarkHelpers.createLivemark());
  }

  /*
   * Tests for fetchSince
   */  
  public void testFetchSinceOneRecord() {
    fetchSinceOneRecord(BookmarkHelpers.createFolder(),
        BookmarkHelpers.createBookmark2());
  }

  public void testFetchSinceReturnNoRecords() {
    fetchSinceReturnNoRecords(BookmarkHelpers.createBookmark1());
  }

  /*
   * Tests for fetch(guid)
   */
  
  public void testFetchOneRecordByGuid() {
    fetchOneRecordByGuid(BookmarkHelpers.createBookmark1(),
        BookmarkHelpers.createBookmark2());
  }
  
  public void testFetchMultipleRecordsByGuids() {
    BookmarkRecord record0 = BookmarkHelpers.createBookmark1();
    BookmarkRecord record1 = BookmarkHelpers.createBookmark2();
    BookmarkRecord record2 = BookmarkHelpers.createQuery();
    fetchMultipleRecordsByGuids(record0, record1, record2);
  }
  
  public void testFetchNoRecordByGuid() {
    fetchNoRecordByGuid(BookmarkHelpers.createSeparator());
  }
  
    
  /*
   * Test wipe
   */
  public void testWipe() {
    doWipe(BookmarkHelpers.createFolder(), BookmarkHelpers.createBookmark2());
  }

}