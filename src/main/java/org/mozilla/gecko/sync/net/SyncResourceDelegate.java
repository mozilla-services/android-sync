/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.net;

import java.io.BufferedReader;
import java.io.IOException;

import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.util.EntityUtils;

/**
 * Shared abstract class for resource delegate that use the same timeouts
 * and no credentials.
 *
 * @author rnewman
 *
 */
public abstract class SyncResourceDelegate implements ResourceDelegate {

  protected Resource resource;
  public SyncResourceDelegate(Resource resource) {
    this.resource = resource;
  }

  @Override
  public int connectionTimeout() {
    return 30 * 1000;             // Wait 30s for a connection to open.
  }
  @Override
  public int socketTimeout() {
    return 5 * 60 * 1000;         // Wait 5 minutes for data.
  }

  @Override
  public String getCredentials() {
    return null;
  }

  @Override
  public void addHeaders(HttpRequestBase request, DefaultHttpClient client) {
  }

  /**
   * Best-effort attempt to ensure that the entity has been fully consumed.
   */
  public static void consumeEntity(HttpEntity entity) {
    try {
      EntityUtils.consume(entity);
    } catch (Exception e) {
      // Doesn't matter.
    }
  }

  public static void consumeReader(BufferedReader reader) {
    try {
      while ((reader.readLine()) != null) {
      }
    } catch (IOException e) {
      return;
    }
  }
}