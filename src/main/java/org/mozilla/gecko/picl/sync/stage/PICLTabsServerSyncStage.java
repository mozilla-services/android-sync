/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.picl.sync.stage;

import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.mozilla.gecko.picl.sync.PICLConfig;
import org.mozilla.gecko.picl.sync.net.PICLServer0Client;
import org.mozilla.gecko.picl.sync.repositories.PICLRecordTranslator;
import org.mozilla.gecko.picl.sync.repositories.PICLServer0Repository;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.android.FennecTabsRepository;
import org.mozilla.gecko.sync.repositories.domain.Record;
import org.mozilla.gecko.sync.repositories.domain.TabsRecord;

/**
 * A <code>PICLServerSyncStage</code> that syncs local tabs to a remote PICL
 * server.
 */
public class PICLTabsServerSyncStage extends PICLServerSyncStage {
  public final static String LOG_TAG = PICLTabsServerSyncStage.class.getSimpleName();

  private static final String COLLECTION = "tabs";

  public PICLTabsServerSyncStage(PICLConfig config, PICLServerSyncStageDelegate delegate) {
    super(config, delegate);
  }


  @Override
  protected Repository makeRemoteRepository() {
    return new PICLServer0Repository(new PICLServer0Client(config.serverURL, config.kA, COLLECTION), new PICLRecordTranslator() {

      @Override
      public ExtendedJSONObject fromRecord(Record record) {
        TabsRecord tabsRecord = (TabsRecord) record;

        ExtendedJSONObject json = new ExtendedJSONObject();
        json.put("id", tabsRecord.guid);

        ExtendedJSONObject payload = new ExtendedJSONObject();
        tabsRecord.populatePayload(payload);

        json.put("payload", payload.toJSONString());

        return json;
      }

      @Override
      public Record toRecord(ExtendedJSONObject json) throws NonObjectJSONException, IOException, ParseException {
        TabsRecord tabsRecord = new TabsRecord();

        tabsRecord.guid = (String) json.get("id");
        ExtendedJSONObject payload = ExtendedJSONObject.parseJSONObject((String) json.get("payload"));
        tabsRecord.initFromPayload(payload);

        return tabsRecord;

      }

    });
  }

  @Override
  protected Repository makeLocalRepository() {
    return new FennecTabsRepository(config.getClientName(), config.getClientGUID());
  }


  @Override
  public String name() {
    return COLLECTION;
  }
}
