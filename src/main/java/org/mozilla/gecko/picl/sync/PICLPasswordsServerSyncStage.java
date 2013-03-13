package org.mozilla.gecko.picl.sync;

import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.picl.sync.net.PICLServer0Client;
import org.mozilla.gecko.picl.sync.repositories.PICLRecordTranslator;
import org.mozilla.gecko.picl.sync.repositories.PICLServer0Repository;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.android.PasswordsRepositorySession.PasswordsRepository;
import org.mozilla.gecko.sync.repositories.domain.PasswordRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

/**
 * A <code>PICLServerSyncStage</code> that syncs local passwords to a remote PICL
 * server.
 */
public class PICLPasswordsServerSyncStage extends PICLServerSyncStage {
  public final static String LOG_TAG = PICLPasswordsServerSyncStage.class.getSimpleName();

  public PICLPasswordsServerSyncStage(PICLConfig config, PICLServerSyncStageDelegate delegate) {
    super(config, delegate);
  }

  @Override
  protected Repository makeLocalRepository() {
    return new PICLServer0Repository(new PICLServer0Client(config.serverURL, config.kA, "passwords"), new PICLRecordTranslator() {

      @Override
      public ExtendedJSONObject fromRecord(Record record) {
        PasswordRecord pRecord = (PasswordRecord) record;

        ExtendedJSONObject json = new ExtendedJSONObject();
        json.put("id", pRecord.guid);

        ExtendedJSONObject payload = new ExtendedJSONObject();
        pRecord.populatePayload(payload);

        json.put("payload", payload.toJSONString());

        return json;
      }

      @Override
      public Record toRecord(ExtendedJSONObject json) throws NonObjectJSONException, IOException, ParseException {
        PasswordRecord record = new PasswordRecord();

        record.guid = (String) json.get("id");
        ExtendedJSONObject payload = ExtendedJSONObject.parseJSONObject((String) json.get("payload"));
        record.initFromPayload(payload);

        return record;
      }

    });
  }

  @Override
  protected Repository makeRemoteRepository() {
    return new PasswordsRepository();
  }

}
