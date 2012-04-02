package org.mozilla.gecko.sync.repositories;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.mozilla.gecko.sync.CredentialsSource;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;

import android.content.Context;

public class FillingServer11Repository extends ConstrainedServer11Repository {
  public static final String LOG_TAG = "FillingServer11Repo";

  public FillingServer11Repository(String serverURI, String username,
      String collection, CredentialsSource credentialsSource, long limit,
      String sort) throws URISyntaxException {
    super(serverURI, username, collection, credentialsSource, limit, sort);
  }

  @Override
  public void createSession(RepositorySessionCreationDelegate delegate, Context context) {
    delegate.onSessionCreated(new FillingServer11RepositorySession(this, context));
  }

  // public since used for testing.
  public String getFileName() {
    return this.collection + ".guids";
  }

  /**
   * Persist old guids remaining to fill.
   *
   * @param guids The guids to persist.
   * @throws Exception
   */
  public void persistGuidsRemaining(String[] guids, Context context) throws Exception {
    try {
      FileOutputStream fos = context.openFileOutput(getFileName(), Context.MODE_PRIVATE);
      for (String guid : guids) {
        fos.write(guid.getBytes("UTF-8"));
        fos.write('\n');
      }
      fos.close();
      Logger.debug(LOG_TAG, "Persisted " + guids.length + " guids to file " + getFileName());
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Got exception persisting " + guids.length + " guids to file " + getFileName(), e);
    }
  }

  /**
   * Fetch old guids remaining to fill.
   *
   * @return The persisted guids, with the convention that <code>null</code>
   *         means that guids were not previously persisted (or could not be
   *         fetched) and an empty list means that guids were previously
   *         persisted but there are none.
   * @throws Exception
   */
  public String[] guidsRemaining(Context context) throws Exception {
    ArrayList<String> guids = new ArrayList<String>();
    InputStream fis;
    try {
      fis = context.openFileInput(getFileName());
    } catch (FileNotFoundException e) {
      return null;
    }
    BufferedReader in = new BufferedReader(new InputStreamReader(fis));
    String s;
    while ((s = in.readLine()) != null) {
      guids.add(s);
    }
    fis.close();
    return guids.toArray(new String[0]);
  }

  /**
   * Return a list of guids to fill this session.
   *
   * @param remainingGuids The list of guids remaining to fill.
   * @param numberOfGuidsAlreadyFetched The number of guids already fetched (not filled) this session.
   * @return The guids to fill this session.
   */
  public String[] guidsToFillThisSession(String[] remainingGuids, int numberOfGuidsAlreadyFetched) {
    int limit = (int) getDefaultFetchLimit();
    if (numberOfGuidsAlreadyFetched >= limit) {
      Logger.debug(LOG_TAG, "Already fetched at least " + limit + " guids this session; not fetching any more.");
      return null;
    }

    if (remainingGuids == null || remainingGuids.length == 0) {
      Logger.debug(LOG_TAG, "No old guids remaining to fill  this session; not fetching any more.");
      return null;
    }

    // We have some left over capacity -- maybe fill in old guids.
    int numGuidsToFill = limit - numberOfGuidsAlreadyFetched; // Usually, we just want to "top off" to the limit.
    numGuidsToFill = Math.max(numGuidsToFill, getDefaultPerFillMinimum());
    numGuidsToFill = Math.min(numGuidsToFill, getDefaultPerFillMaximum());
    numGuidsToFill = Math.min(numGuidsToFill, remainingGuids.length);
    if (numGuidsToFill < 1) {
      return null;
    }

    String[] guidsToFillList = new String[numGuidsToFill];
    System.arraycopy(remainingGuids, 0, guidsToFillList, 0, numGuidsToFill);
    return guidsToFillList;
  }

  /**
   * The maximum numbers of guids to fetch from the server per fill. Set this so
   * that each fill is "not too dear".
   * <p>
   * Override this in subclasses.
   *
   * @return
   */
  protected int getDefaultPerFillMaximum() {
    return (int) getDefaultFetchLimit();
  }

  /**
   * The minimum numbers of guids to fetch from the server per fill. Set this so
   * that each fill is "worth your while".
   * <p>
   * Override this in subclasses.
   *
   * @return
   */
  protected int getDefaultPerFillMinimum() {
    return 100;
  }
}
