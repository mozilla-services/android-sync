/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories.domain;

import org.json.simple.JSONArray;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.NonArrayJSONException;
import org.mozilla.gecko.sync.repositories.RecordFactory;

public class ClientRecordFactory extends RecordFactory {
  private static final String LOG_TAG = "RecordFactory";

  @Override
  public Record createRecord(Record record) {
    ExtendedJSONObject p = ((CryptoRecord)record).payload;

    String guid = record.guid;
    String name = (String) p.get("name");
    String type = (String) p.get("type");

    JSONArray commands = null;
    try {
      commands = p.getArray("commands");
    } catch (NonArrayJSONException e) {
      Logger.debug(LOG_TAG, "Got non-array commands in client record " + guid, e);
      // Keep commands as null.
    }

    ClientRecord r = new ClientRecord(guid, name, type, commands);
    r.initFromPayload((CryptoRecord) record);
    return r;
  }
}
