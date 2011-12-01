package org.mozilla.android.sync.test;

import org.mozilla.android.sync.repositories.android.AndroidBrowserBookmarksDatabaseHelper;
import org.mozilla.android.sync.repositories.android.AndroidBrowserBookmarksRepository;
import org.mozilla.android.sync.repositories.android.AndroidBrowserRepository;
import org.mozilla.android.sync.repositories.android.AndroidBrowserRepositoryDatabaseHelper;

public class AndroidBookmarksTestBase extends AndroidRepositoryTest {

  @Override
  protected AndroidBrowserRepository getRepository() {
    return new AndroidBrowserBookmarksRepository();
  }
  
  @Override
  protected AndroidBrowserRepositoryDatabaseHelper getDatabaseHelper() {
    return new AndroidBrowserBookmarksDatabaseHelper(getApplicationContext());
  }
  
}
