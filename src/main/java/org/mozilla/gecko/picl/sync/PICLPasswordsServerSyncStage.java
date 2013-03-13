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
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Record toRecord(ExtendedJSONObject str)
          throws NonObjectJSONException, IOException, ParseException {
        // TODO Auto-generated method stub
        return null;
      }

    });
  }

  @Override
  protected Repository makeRemoteRepository() {
    return new PasswordsRepository();
  }

}
