/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import java.net.URI;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.gecko.background.testhelpers.MockSharedPreferences;
import org.mozilla.gecko.sync.PrefsSource;
import org.mozilla.gecko.sync.SyncConfiguration;

import android.content.SharedPreferences;

public class TestSyncConfiguration implements PrefsSource {

  @Override
  public SharedPreferences getPrefs(String name, int mode) {
    return new MockSharedPreferences();
  }

  @Test
  public void testURLs() throws Exception {
    SyncConfiguration config = new SyncConfiguration("username", null, "prefsPath", this);
    config.clusterURL = new URI("https://db.com/internal/");
    Assert.assertEquals("https://db.com/internal/1.1/username/info/collections", config.infoCollectionsURL());
    Assert.assertEquals("https://db.com/internal/1.1/username/info/collection_counts", config.infoCollectionCountsURL());
    Assert.assertEquals("https://db.com/internal/1.1/username/storage/meta/global", config.metaURL());
    Assert.assertEquals("https://db.com/internal/1.1/username/storage", config.storageURL());
    Assert.assertEquals("https://db.com/internal/1.1/username/storage/collection", config.collectionURI("collection").toASCIIString());
  }
}
