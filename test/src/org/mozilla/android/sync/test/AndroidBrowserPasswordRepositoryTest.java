/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.android.sync.test.helpers.PasswordHelpers;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserPasswordsDataAccessor;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserPasswordsRepository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepositoryDataAccessor;
import org.mozilla.gecko.sync.repositories.domain.PasswordRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.util.Log;

public class AndroidBrowserPasswordRepositoryTest extends AndroidBrowserRepositoryTest {

  // Hacky overrides to avoid test failures until passwords lands.

  @Override
  protected AndroidBrowserRepository getRepository() {
    return new AndroidBrowserPasswordsRepository();
  }

  @Override
  protected AndroidBrowserRepositoryDataAccessor getDataAccessor() {
    return new AndroidBrowserPasswordsDataAccessor(getApplicationContext());
  }

  @Override
  public void testFetchAll() {
    Record[] expected = new Record[2];
    expected[0] = PasswordHelpers.createPassword1();
    expected[1] = PasswordHelpers.createPassword2();
    basicFetchAllTest(expected);
  }

  @Override
  public void testGuidsSinceReturnMultipleRecords() {
    PasswordRecord record0 = PasswordHelpers.createPassword3();
    PasswordRecord record1 = PasswordHelpers.createPassword2(); 
    guidsSinceReturnMultipleRecords(record0, record1);
  }

  @Override
  public void testGuidsSinceReturnNoRecords() {
    guidsSinceReturnNoRecords(PasswordHelpers.createPassword1());
  }

  @Override
  public void testFetchSinceOneRecord() {
    RepositorySession session = createAndBeginSession();

    PasswordRecord record1 = PasswordHelpers.createPassword1();
    // Passwords fetchSince uses timePasswordChanged, not
    long time1 = System.currentTimeMillis();
    record1.timePasswordChanged = time1;
    performWait(storeRunnable(session, record1));

    PasswordRecord record2 = PasswordHelpers.createPassword2();
    long time2 = System.currentTimeMillis();
    record2.timePasswordChanged = time2;
    performWait(storeRunnable(session, record2));

    Log.i("fetchSinceOneRecord", "Fetching record 1.");
    String[] expectedOne = new String[] { record2.guid };
    performWait(fetchSinceRunnable(session, time2, expectedOne));

    Log.i("fetchSinceOneRecord", "Fetching both, relying on inclusiveness.");
    String[] expectedBoth = new String[] { record1.guid, record2.guid };
    performWait(fetchSinceRunnable(session, time1, expectedBoth));

    Log.i("fetchSinceOneRecord", "Done.");
    dispose(session);
  }

  @Override
  public void testFetchSinceReturnNoRecords() {
    fetchSinceReturnNoRecords(PasswordHelpers.createPassword2());
  }

  @Override
  public void testFetchOneRecordByGuid() {
    fetchOneRecordByGuid(PasswordHelpers.createPassword1(),
                         PasswordHelpers.createPassword2());
  }

  @Override
  public void testFetchMultipleRecordsByGuids() {
    PasswordRecord record0 = PasswordHelpers.createPassword1();
    PasswordRecord record1 = PasswordHelpers.createPassword2();
    PasswordRecord record2 = PasswordHelpers.createPassword3();
    fetchMultipleRecordsByGuids(record0, record1, record2);
  }

  @Override
  public void testFetchNoRecordByGuid() {
    fetchNoRecordByGuid(PasswordHelpers.createPassword1());
  }

  @Override
  public void testWipe() {
    doWipe(PasswordHelpers.createPassword2(), PasswordHelpers.createPassword3());
  }

  @Override
  public void testStore() {
    final RepositorySession session = createAndBeginSession();
    performWait(storeRunnable(session, PasswordHelpers.createPassword1()));
  }

  @Override
  public void testRemoteNewerTimeStamp() {
    PasswordRecord local = PasswordHelpers.createPassword2();
    PasswordRecord remote = PasswordHelpers.createPassword1();
    remoteNewerTimeStamp(local, remote);
  }

  @Override
  public void testLocalNewerTimeStamp() {
    PasswordRecord local = PasswordHelpers.createPassword1();
    PasswordRecord remote = PasswordHelpers.createPassword2();
    localNewerTimeStamp(local, remote);
  }

  @Override
  public void testDeleteRemoteNewer() {
    PasswordRecord local = PasswordHelpers.createPassword1();
    PasswordRecord remote = PasswordHelpers.createPassword2();
    deleteRemoteNewer(local, remote);
  }

  @Override
  public void testDeleteLocalNewer() {
    PasswordRecord local = PasswordHelpers.createPassword1();
    PasswordRecord remote = PasswordHelpers.createPassword3();
    deleteLocalNewer(local, remote);
  }

  @Override
  public void testDeleteRemoteLocalNonexistent() {
    deleteRemoteLocalNonexistent(PasswordHelpers.createPassword5());
  }

  @Override
  public void testStoreIdenticalExceptGuid() {
    storeIdenticalExceptGuid(PasswordHelpers.createPassword1());
  }

  @Override
  public void testCleanMultipleRecords() {
     cleanMultipleRecords(
        PasswordHelpers.createPassword1(),
        PasswordHelpers.createPassword2(),
        PasswordHelpers.createPassword3(),
        PasswordHelpers.createPassword4(),
        PasswordHelpers.createPassword5()
    );
  }
}
