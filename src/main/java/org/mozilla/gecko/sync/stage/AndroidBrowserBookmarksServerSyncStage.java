/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.stage;

import java.net.URISyntaxException;

import org.mozilla.gecko.sync.repositories.ConstrainedServer11Repository;
import org.mozilla.gecko.sync.repositories.RecordFactory;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserBookmarksRepository;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecordFactory;

public class AndroidBrowserBookmarksServerSyncStage extends ServerSyncStage {

  // Eventually this kind of sync stage will be data-driven,
  // and all this hard-coding can go away.
  private static final String BOOKMARKS_SORT          = "index";
  private static final long   BOOKMARKS_REQUEST_LIMIT = 5000;         // Sanity limit.

  @Override
  public void execute(org.mozilla.gecko.sync.GlobalSession session) throws NoSuchStageException {
    super.execute(session);
  }

  @Override
  protected String getCollection() {
    return "bookmarks";
  }
  @Override
  protected String getEngineName() {
    return "bookmarks";
  }

  @Override
  protected Repository getRemoteRepository() throws URISyntaxException {
    return new ConstrainedServer11Repository(session.config.getClusterURLString(),
                                             session.config.username,
                                             getCollection(),
                                             session,
                                             BOOKMARKS_REQUEST_LIMIT,
                                             BOOKMARKS_SORT);
  }

  @Override
  protected Repository getLocalRepository() {
    return new AndroidBrowserBookmarksRepository();
  }

  @Override
  protected RecordFactory getRecordFactory() {
    return new BookmarkRecordFactory();
  }
}