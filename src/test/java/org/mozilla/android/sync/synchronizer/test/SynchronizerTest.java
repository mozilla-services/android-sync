package org.mozilla.android.sync.synchronizer.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.synchronizer.Synchronizer;
import org.mozilla.android.sync.synchronizer.SynchronizerSession;
import org.mozilla.android.sync.synchronizer.SynchronizerSessionDelegate;
import org.mozilla.android.sync.test.WBORepository;

import android.content.Context;

public class SynchronizerTest {

  public static void recordEquals(BookmarkRecord r, String guid, long lastModified, boolean deleted, String collection) {
    assertEquals(r.guid,         guid);
    assertEquals(r.lastModified, lastModified);
    assertEquals(r.deleted,      deleted);
    assertEquals(r.collection,   collection);
  }

  public static void recordEquals(BookmarkRecord a, BookmarkRecord b) {
    assertEquals(a.guid,         b.guid);
    assertEquals(a.lastModified, b.lastModified);
    assertEquals(a.deleted,      b.deleted);
    assertEquals(a.collection,   b.collection);
  }

  @Test
  public void test() {    
    Context context = null;
    final WBORepository repoA = new WBORepository();
    final WBORepository repoB = new WBORepository();

    final String collection  = "bookmarks";
    final boolean deleted    = false;
    final String guidA       = "abcdabcdabcd";
    final String guidB       = "ffffffffffff";
    final long lastModifiedA = 312345;
    final long lastModifiedB = 412345;
    BookmarkRecord bookmarkRecordA = new BookmarkRecord(guidA, collection, lastModifiedA, deleted);
    BookmarkRecord bookmarkRecordB = new BookmarkRecord(guidB, collection, lastModifiedB, deleted);

    repoA.wbos.put(guidA, bookmarkRecordA);
    repoB.wbos.put(guidB, bookmarkRecordB);
    Synchronizer synchronizer = new Synchronizer();
    synchronizer.repositoryA = repoA;
    synchronizer.repositoryB = repoB;
    SynchronizerSession syncSession = new SynchronizerSession(synchronizer, new SynchronizerSessionDelegate() {
      
      @Override
      public void onInitialized(SynchronizerSession session) {
        assertFalse(repoA.wbos.containsKey(guidB));
        assertFalse(repoB.wbos.containsKey(guidA));
        assertTrue(repoA.wbos.containsKey(guidA));
        assertTrue(repoB.wbos.containsKey(guidB));
        session.synchronize();
      }

      @Override
      public void onSynchronized(SynchronizerSession session) {
        // Verify contents.
        assertTrue(repoA.wbos.containsKey(guidA));
        assertTrue(repoA.wbos.containsKey(guidB));
        assertTrue(repoB.wbos.containsKey(guidA));
        assertTrue(repoB.wbos.containsKey(guidB));
        BookmarkRecord aa = (BookmarkRecord) repoA.wbos.get(guidA);
        BookmarkRecord ab = (BookmarkRecord) repoA.wbos.get(guidB);
        BookmarkRecord ba = (BookmarkRecord) repoB.wbos.get(guidA);
        BookmarkRecord bb = (BookmarkRecord) repoB.wbos.get(guidB);
        recordEquals(aa, guidA, lastModifiedA, deleted, collection);
        recordEquals(ab, guidB, lastModifiedB, deleted, collection);
        recordEquals(ba, guidA, lastModifiedA, deleted, collection);
        recordEquals(bb, guidB, lastModifiedB, deleted, collection);
        recordEquals(aa, ba);
        recordEquals(ab, bb);
      }
      
      @Override
      public void onSynchronizeFailed(SynchronizerSession session,
                                      Exception lastException, String reason) {
        fail("Synchronization should not fail.");
      }
      
      @Override
      public void onStoreError(Exception e) {
        fail("Should be no store error.");
      }
      
      @Override
      public void onSessionError(Exception e) {
        fail("Should be no session error.");
      }
      
      @Override
      public void onFetchError(Exception e) {
        fail("Should be no fetch error.");
      }
    });
    syncSession.init(context);
  }

}
