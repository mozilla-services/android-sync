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
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserBookmarksRepository;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

public class PICLBookmarksServerSyncStage extends PICLServerSyncStage {

  public PICLBookmarksServerSyncStage(PICLConfig config, PICLServerSyncStageDelegate delegate) {
    super(config, delegate);
  }

  @Override
  protected Repository makeRemoteRepository() {
    return new PICLServer0Repository(new PICLServer0Client(config.serverURL, config.kA, "bookmarks"), new PICLRecordTranslator() {

      @Override
      public ExtendedJSONObject fromRecord(Record record) {
        BookmarkRecord bookmarkRecord = (BookmarkRecord) record;
        
        ExtendedJSONObject json = new ExtendedJSONObject();
        json.put("id", bookmarkRecord.guid);

        ExtendedJSONObject payload = new ExtendedJSONObject();
        bookmarkRecord.populatePayload(payload);

        json.put("payload", payload.toJSONString());
        
        return json;
      }

      @Override
      public Record toRecord(ExtendedJSONObject json) throws NonObjectJSONException, IOException, ParseException {
        BookmarkRecord record = new BookmarkRecord();
        
        record.guid = (String) json.get("id");
        ExtendedJSONObject payload = ExtendedJSONObject.parseJSONObject((String) json.get("payload"));
        record.initFromPayload(payload);
        
        return record;
      }
      
    });
  }

  @Override
  protected Repository makeLocalRepository() {
    return new AndroidBrowserBookmarksRepository();
  }

}
