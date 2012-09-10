/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.mozilla.gecko.sync.CredentialsSource;
import org.mozilla.gecko.sync.Utils;

/**
 * A ServerRepository implements fetching and storing against a Sync API.
 * <p>
 * It doesn't do crypto: that's the job of the middleware.
 */
public abstract class ServerRepository extends Repository implements CredentialsSource {

  protected String serverURI;
  protected String username;
  protected String collection;
  protected String collectionPath;
  protected URI collectionPathURI;
  protected CredentialsSource credentialsSource;

  /**
   * @param serverURI
   *        URI of the Sync server (string)
   * @param version
   *        API version on the server (string)
   * @param username
   *        Username on the server (string)
   * @param collection
   *        Name of the collection (string)
   * @throws URISyntaxException
   */
  public ServerRepository(String serverURI, String version, String username, String collection, CredentialsSource credentialsSource) throws URISyntaxException {
    this.serverURI  = serverURI;
    this.username   = username;
    this.collection = collection;

    this.collectionPath = this.serverURI + version + "/" + this.username + "/storage/" + this.collection;
    this.collectionPathURI = new URI(this.collectionPath);
    this.credentialsSource = credentialsSource;
  }

  @Override
  public String credentials() {
    return this.credentialsSource.credentials();
  }

  public URI collectionURI(boolean full, long newer, long limit, String sort, String ids) throws URISyntaxException {
    ArrayList<String> params = new ArrayList<String>();
    if (full) {
      params.add("full=1");
    }
    if (newer >= 0) {
      // Translate local millisecond timestamps into server decimal seconds.
      String newerString = Utils.millisecondsToDecimalSecondsString(newer);
      params.add("newer=" + newerString);
    }
    if (limit > 0) {
      params.add("limit=" + limit);
    }
    if (sort != null) {
      params.add("sort=" + sort);       // We trust these values.
    }
    if (ids != null) {
      params.add("ids=" + ids);         // We trust these values.
    }

    if (params.size() == 0) {
      return this.collectionPathURI;
    }

    StringBuilder out = new StringBuilder();
    char indicator = '?';
    for (String param : params) {
      out.append(indicator);
      indicator = '&';
      out.append(param);
    }
    String uri = this.collectionPath + out.toString();
    return new URI(uri);
  }

  public URI collectionURI() {
    return this.collectionPathURI;
  }

  public URI wboURI(String id) throws URISyntaxException {
    return new URI(this.collectionPath + "/" + id);
  }

  // Override these.
  protected long getDefaultFetchLimit() {
    return -1;
  }

  protected String getDefaultSort() {
    return null;
  }
}
