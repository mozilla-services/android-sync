package org.mozilla.android.sync.test;

import org.mozilla.android.sync.repositories.android.AndroidBrowserHistoryDatabaseHelper;
import org.mozilla.android.sync.repositories.android.AndroidBrowserHistoryRepository;
import org.mozilla.android.sync.repositories.android.AndroidBrowserRepository;
import org.mozilla.android.sync.repositories.android.AndroidBrowserRepositoryDatabaseHelper;

public class AndroidHistoryTestBase extends AndroidRepositoryTest {

  @Override
  protected AndroidBrowserRepository getRepository() {
    return new AndroidBrowserHistoryRepository();
  }

  @Override
  protected AndroidBrowserRepositoryDatabaseHelper getDatabaseHelper() {
    return new AndroidBrowserHistoryDatabaseHelper(getApplicationContext());
  }

}
