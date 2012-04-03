/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.stage;

import java.net.URISyntaxException;

import org.mozilla.gecko.sync.repositories.FillingServer11Repository;
import org.mozilla.gecko.sync.repositories.RecordFactory;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserHistoryRepository;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecordFactory;

public class AndroidBrowserHistoryServerSyncStage extends ServerSyncStage {
  // Eventually this kind of sync stage will be data-driven,
  // and all this hard-coding can go away.
  private static final String HISTORY_SORT          = "newest"; // So that we fill recent guids first.
  private static final long   HISTORY_REQUEST_LIMIT = 20;
  private static final int    HISTORY_REQUEST_FILL_MAXIMUM = 10;
  private static final int    HISTORY_REQUEST_FILL_MINIMUM = 5;

  @Override
  protected String getCollection() {
    return "history";
  }

  @Override
  protected String getEngineName() {
    return "history";
  }

  @Override
  protected Repository getLocalRepository() {
    return new AndroidBrowserHistoryRepository();
  }

  @Override
  protected Repository getRemoteRepository() throws URISyntaxException {
    return new FillingServer11Repository(session.config.getClusterURLString(),
        session.config.username,
        getCollection(),
        session,
        HISTORY_REQUEST_LIMIT,
        HISTORY_SORT) {
      @Override
      protected int getDefaultPerFillMaximum() {
        return HISTORY_REQUEST_FILL_MAXIMUM;
      }

      @Override
      protected int getDefaultPerFillMinimum() {
        return HISTORY_REQUEST_FILL_MINIMUM;
      }
    };
  }

  @Override
  protected RecordFactory getRecordFactory() {
    return new HistoryRecordFactory();
  }
}
