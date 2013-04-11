package org.mozilla.gecko.picl.sync.stage;

import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.picl.sync.PICLConfig;
import org.mozilla.gecko.picl.sync.net.PICLServer0Client;
import org.mozilla.gecko.picl.sync.repositories.PICLRecordTranslator;
import org.mozilla.gecko.picl.sync.repositories.PICLServer0Repository;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserHistoryRepository;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

public class PICLHistoryServerSyncStage extends PICLServerSyncStage {

  public static final String COLLECTION = "history";

  public PICLHistoryServerSyncStage(PICLConfig config, PICLServerSyncStageDelegate delegate) {
    super(config, delegate);
  }

  @Override
  protected Repository makeLocalRepository() {
    return new AndroidBrowserHistoryRepository();
  }

  @Override
  protected Repository makeRemoteRepository() {
    return new PICLServer0Repository(new PICLServer0Client(config.serverURL, config.kA, COLLECTION), new PICLRecordTranslator() {

      @Override
      public ExtendedJSONObject fromRecord(Record record) {
        HistoryRecord historyRecord = (HistoryRecord) record;

        ExtendedJSONObject json = new ExtendedJSONObject();

        json.put("id", historyRecord.guid);
        ExtendedJSONObject payload = new ExtendedJSONObject();
        historyRecord.populatePayload(payload);

        json.put("payload", payload.toJSONString());

        return json;
      }

      @Override
      public Record toRecord(ExtendedJSONObject json) throws NonObjectJSONException, IOException, ParseException {
        HistoryRecord record = new HistoryRecord();

        record.guid = (String) json.get("id");
        ExtendedJSONObject payload = ExtendedJSONObject.parseJSONObject((String) json.get("payload"));
        record.initFromPayload(payload);

        return record;
      }

    });
  }

  @Override
  public String name() {
    return COLLECTION;
  }

}
