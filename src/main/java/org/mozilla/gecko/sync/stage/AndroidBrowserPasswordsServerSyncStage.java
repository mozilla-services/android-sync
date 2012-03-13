/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.stage;

import java.net.URISyntaxException;

import org.mozilla.gecko.sync.repositories.ConstrainedServer11Repository;
import org.mozilla.gecko.sync.repositories.RecordFactory;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserPasswordsRepository;
import org.mozilla.gecko.sync.repositories.domain.PasswordRecordFactory;

public class AndroidBrowserPasswordsServerSyncStage extends ServerSyncStage {

  private static final String PASSWORDS_SORT          = "index";
  private static final long   PASSWORDS_REQUEST_LIMIT = 100;

  @Override
  public void execute(org.mozilla.gecko.sync.GlobalSession session) throws NoSuchStageException {
    super.execute(session);
  }

  @Override
  protected String getCollection() {
    return "passwords";
  }
  @Override
  protected String getEngineName() {
    return "passwords";
  }

  @Override
  protected Repository getLocalRepository() {
    return new AndroidBrowserPasswordsRepository();
  }

  @Override
  protected Repository getRemoteRepository() throws URISyntaxException {
    return new ConstrainedServer11Repository(session.config.getClusterURLString(),
                                             session.config.username,
                                             getCollection(),
                                             session,
                                             PASSWORDS_REQUEST_LIMIT,
                                             PASSWORDS_SORT);
  }

  @Override
  protected RecordFactory getRecordFactory() {
    return new PasswordRecordFactory();
  }

}
