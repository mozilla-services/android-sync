/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories.domain;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonArrayJSONException;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.android.RepoUtils;

import android.content.ContentValues;

// Immutable.
public class Tab {
  public final String    title;
  public final String    icon;
  public final JSONArray history;
  public final long      lastUsed;

  public Tab(String title, String icon, JSONArray history, long lastUsed) {
    this.title    = title;
    this.icon     = icon;
    this.history  = history;
    this.lastUsed = lastUsed;
  }

  public static Tab fromJSONObject(JSONObject o) throws NonArrayJSONException {
    ExtendedJSONObject obj = new ExtendedJSONObject(o);
    String title      = obj.getString("title");
    String icon       = obj.getString("icon");
    JSONArray history = obj.getArray("urlHistory");

    // Last used is inexplicably a string in seconds. Most of the time.
    long lastUsed = 0;
    Object lU = obj.get("lastUsed");
    if (lU instanceof Number) {
      lastUsed = ((Long) lU) * 1000L;
    } else if (lU instanceof String) {
      try {
        lastUsed = Long.parseLong((String) lU, 10) * 1000L;
      } catch (NumberFormatException e) {
        Logger.debug(TabsRecord.LOG_TAG, "Invalid number format in lastUsed: " + lU);
      }
    }
    return new Tab(title, icon, history, lastUsed);
  }

  @SuppressWarnings("unchecked")
  public JSONObject toJSONObject() {
    JSONObject o = new JSONObject();
    o.put("title", title);
    o.put("icon", icon);
    o.put("urlHistory", history);
    o.put("lastUsed", this.lastUsed / 1000);
    return o;
  }

  public ContentValues toContentValues(String clientGUID, int position) {
    ContentValues out = new ContentValues();
    out.put(BrowserContract.Tabs.POSITION,    position);
    out.put(BrowserContract.Tabs.CLIENT_GUID, clientGUID);

    out.put(BrowserContract.Tabs.FAVICON,   this.icon);
    out.put(BrowserContract.Tabs.LAST_USED, this.lastUsed);
    out.put(BrowserContract.Tabs.TITLE,     this.title);
    out.put(BrowserContract.Tabs.URL,       (String) this.history.get(0));
    out.put(BrowserContract.Tabs.HISTORY,   this.history.toJSONString());
    return out;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Tab)) {
      return false;
    }
    final Tab other = (Tab) o;

    if (!RepoUtils.stringsEqual(this.title, other.title)) {
      return false;
    }
    if (!RepoUtils.stringsEqual(this.icon, other.icon)) {
      return false;
    }

    if (!(this.lastUsed == other.lastUsed)) {
      return false;
    }

    return Utils.sameArrays(this.history, other.history);
  }
}