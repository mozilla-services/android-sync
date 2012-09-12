package org.mozilla.gecko.sync.repositories;

import java.util.ArrayList;

import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionStoreDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionWipeDelegate;

public class Server20RepositorySession extends ServerRepositorySession {
  public static final String LOG_TAG = "Server20RepoSess";

  public Server20RepositorySession(final Repository repository) {
    super(repository);
  }

  @Override
  protected Runnable makeUploadRunnable(
      RepositorySessionStoreDelegate storeDelegate, ArrayList<byte[]> outgoing,
      ArrayList<String> outgoingGuids, long byteCount) {

    return new Runnable() {
      @Override
      public void run() {
        Logger.warn(LOG_TAG, "Uploading not yet implemented.");
      }
    };
  }

  @Override
  public void fetchSince(final long timestamp,
      final RepositorySessionFetchRecordsDelegate delegate) {
    delegateQueue.execute(new Runnable() {
      @Override
      public void run() {
        Logger.warn(LOG_TAG, "fetchSince not yet implemented.");
        delegate.onFetchCompleted(System.currentTimeMillis());
      }
    });
  }

  @Override
  public void fetchAll(final RepositorySessionFetchRecordsDelegate delegate) {
    fetchSince(0, delegate);
  }

  @Override
  public void fetch(final String[] guids,
      final RepositorySessionFetchRecordsDelegate delegate)
      throws InactiveSessionException {
    delegateQueue.execute(new Runnable() {
      @Override
      public void run() {
        Logger.warn(LOG_TAG, "fetch not yet implemented.");
        delegate.onFetchCompleted(System.currentTimeMillis());
      }
    });
  }


  @Override
  public void guidsSince(final long timestamp,
      final RepositorySessionGuidsSinceDelegate delegate) {
    delegateQueue.execute(new Runnable() {
      @Override
      public void run() {
        Logger.warn(LOG_TAG, "guidsSince not yet implemented.");
        delegate.onGuidsSinceSucceeded(new String[] {});
      }
    });
  }

  @Override
  public void wipe(final RepositorySessionWipeDelegate delegate) {
    delegateQueue.execute(new Runnable() {
      @Override
      public void run() {
        Logger.warn(LOG_TAG, "wipe not yet implemented.");
        delegate.onWipeSucceeded();
      }
    });
  }
}
