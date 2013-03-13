/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.picl.sync.repositories;

import org.mozilla.gecko.picl.sync.net.PICLServer0Client;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;

import android.content.Context;

/**
 * A PICL0ServerRepository implements fetching and storing against the PICL Server ProtocolZero.
 * https://wiki.mozilla.org/Identity/AttachedServices/StorageProtocolZero
 */
public class PICLServer0Repository extends Repository {
  
  
  /*public static enum Collection {
    tabs,
    passwords;
  }*/ 

  public final PICLServer0Client client;
  public final PICLRecordTranslator translator;
  

  public PICLServer0Repository(PICLServer0Client client, PICLRecordTranslator translator) {
    this.client = client;
    this.translator = translator;
  }

  @Override
  public void createSession(RepositorySessionCreationDelegate delegate, Context context) {
    delegate.onSessionCreated(new PICLServer0RepositorySession(this));
  }
}
