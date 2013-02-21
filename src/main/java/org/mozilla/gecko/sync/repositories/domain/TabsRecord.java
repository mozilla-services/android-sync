/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories.domain;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonArrayJSONException;
import org.mozilla.gecko.sync.Utils;

import android.content.ContentValues;

/**
 * Represents a client's collection of tabs.
 *
 * @author rnewman
 *
 */
public class TabsRecord extends Record {
  public static final String LOG_TAG = "TabsRecord";

  public static final String COLLECTION_NAME = "tabs";
  public static final long TABS_TTL = 7 * 24 * 60 * 60; // 7 days in seconds.

  public TabsRecord(String guid, String collection, long lastModified, boolean deleted) {
    super(guid, collection, lastModified, deleted);
    this.ttl = TABS_TTL;
  }
  public TabsRecord(String guid, String collection, long lastModified) {
    this(guid, collection, lastModified, false);
  }
  public TabsRecord(String guid, String collection) {
    this(guid, collection, 0, false);
  }
  public TabsRecord(String guid) {
    this(guid, COLLECTION_NAME, 0, false);
  }
  public TabsRecord() {
    this(Utils.generateGuid(), COLLECTION_NAME, 0, false);
  }

  public String clientName;
  public ArrayList<Tab> tabs;

  @Override
  public void initFromPayload(ExtendedJSONObject payload) {
    clientName = (String) payload.get("clientName");
    try {
      tabs = tabsFrom(payload.getArray("tabs"));
    } catch (NonArrayJSONException e) {
      // Oh well.
      tabs = new ArrayList<Tab>();
    }
  }

  @SuppressWarnings("unchecked")
  protected static JSONArray tabsToJSON(ArrayList<Tab> tabs) {
    JSONArray out = new JSONArray();
    for (Tab tab : tabs) {
      out.add(tab.toJSONObject());
    }
    return out;
  }

  protected static ArrayList<Tab> tabsFrom(JSONArray in) {
    ArrayList<Tab> tabs = new ArrayList<Tab>(in.size());
    for (Object o : in) {
      if (o instanceof JSONObject) {
        try {
          tabs.add(Tab.fromJSONObject((JSONObject) o));
        } catch (NonArrayJSONException e) {
          Logger.warn(LOG_TAG, "urlHistory is not an array for this tab.", e);
        }
      }
    }
    return tabs;
  }

  @Override
  public void populatePayload(ExtendedJSONObject payload) {
    putPayload(payload, "id", this.guid);
    putPayload(payload, "clientName", this.clientName);
    payload.put("tabs", tabsToJSON(this.tabs));
  }

  @Override
  public Record copyWithIDs(String guid, long androidID) {
    TabsRecord out = new TabsRecord(guid, this.collection, this.lastModified, this.deleted);
    out.androidID = androidID;
    out.sortIndex = this.sortIndex;
    out.ttl       = this.ttl;

    out.clientName = this.clientName;
    out.tabs = new ArrayList<Tab>(this.tabs);

    return out;
  }

  public ContentValues getClientsContentValues() {
    ContentValues cv = new ContentValues();
    cv.put(BrowserContract.Clients.GUID, this.guid);
    cv.put(BrowserContract.Clients.NAME, this.clientName);
    cv.put(BrowserContract.Clients.LAST_MODIFIED, this.lastModified);
    return cv;
  }

  public ContentValues[] getTabsContentValues() {
    int c = tabs.size();
    ContentValues[] out = new ContentValues[c];
    for (int i = 0; i < c; i++) {
      out[i] = tabs.get(i).toContentValues(this.guid, i);
    }
    return out;
  }
}
