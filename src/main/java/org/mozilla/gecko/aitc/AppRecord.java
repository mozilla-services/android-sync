package org.mozilla.gecko.aitc;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.mozilla.apache.commons.codec.binary.Base64;
import org.mozilla.apache.commons.codec.digest.DigestUtils;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonArrayJSONException;

public class AppRecord {
  public final String name;
  public final List<String> receipts;

  public final String origin;
  public final String manifestPath;
  public final String installOrigin;

  public final long installedAt;
  public final long modifiedAt;

  public final boolean hidden;

  public AppRecord(String name, String origin, String manifestPath, String installOrigin) {
    this(name, origin, manifestPath, installOrigin, new ArrayList<String>(), 0, 0, false);
  }

  // For internal use and testing.
  protected AppRecord(String name, String origin, String manifestPath, String installOrigin,
      ArrayList<String> receipts, long installedAt, long modifiedAt, boolean hidden) {
    this.name = name;
    this.receipts = receipts;

    this.origin = origin;
    this.manifestPath = manifestPath;
    this.installOrigin = installOrigin;

    this.installedAt = installedAt;
    this.modifiedAt = modifiedAt;

    this.hidden = hidden;
  }

  public static String appId(String origin) {
    return Base64.encodeBase64URLSafeString(DigestUtils.sha(origin));
  }

  public String appId() {
    return appId(origin);
  }

  @Override
  public String toString() {
    return toJSONString();
  }

  public String toJSONString() {
    return toJSON().toJSONString();
  }

  @SuppressWarnings("unchecked")
  public ExtendedJSONObject toJSON() {
    ExtendedJSONObject o = new ExtendedJSONObject();

    o.put("name", name);

    JSONArray receiptsArray = new JSONArray();
    receiptsArray.addAll(receipts);
    o.put("receipts", receiptsArray);

    o.put("origin", origin);
    o.put("manifestPath", manifestPath);
    o.put("installOrigin", installOrigin);

    if (installedAt > 0) {
      o.put("installedAt", installedAt);
    }
    if (modifiedAt > 0) {
      o.put("modifiedAt", modifiedAt);
    }

    if (hidden) {
      o.put("hidden", hidden);
    }

    return o;
  }

  @SuppressWarnings("unchecked")
  public static AppRecord fromJSONObject(ExtendedJSONObject o) throws NonArrayJSONException {
    String name = o.getString("name");

    ArrayList<String> receipts = null;
    JSONArray receiptsArray = o.getArray("receipts");
    if (receiptsArray != null) {
      receipts = new ArrayList<String>();
      receipts.addAll(receiptsArray);
    }

    String origin = o.getString("origin");
    String manifestPath = o.getString("manifestPath");
    String installOrigin = o.getString("installOrigin");

    long installedAt = 0;
    Long installedAtLong = o.getLong("installedAt");
    if (installedAtLong != null) {
      installedAt = installedAtLong.longValue();
    }

    long modifiedAt = 0;
    Long modifiedAtLong = o.getLong("modifiedAt");
    if (modifiedAtLong != null) {
      modifiedAt = modifiedAtLong.longValue();
    }

    boolean hidden = false;
    Object hiddenObject = o.get("hidden");
    if (hiddenObject instanceof Boolean && ((Boolean) hiddenObject).booleanValue()) {
      hidden = true;
    }

    return new AppRecord(name, origin, manifestPath, installOrigin, receipts, installedAt, modifiedAt, hidden);
  }
}