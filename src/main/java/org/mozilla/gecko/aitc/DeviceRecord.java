package org.mozilla.gecko.aitc;

import java.util.UUID;

import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;

public class DeviceRecord {
  public final String uuid;
  public final String name;

  public final String type;
  public final String layout;

  public final long addedAt;
  public final long modifiedAt;

  public final ExtendedJSONObject apps;

  public DeviceRecord(String uuid, String name, String type, String layout, ExtendedJSONObject apps) {
    this(uuid, name, type, layout, apps, 0, 0);
  }

  // For internal use and testing.
  protected DeviceRecord(String uuid, String name, String type, String layout, ExtendedJSONObject apps, long addedAt, long modifiedAt) {
    this.uuid = uuid.toUpperCase();
    this.name = name;

    this.type = type;
    this.layout = layout;

    this.addedAt = addedAt;
    this.modifiedAt = modifiedAt;

    this.apps = (apps != null) ? apps.clone() : apps;
  }

  public static String generateUUID() {
    return UUID.randomUUID().toString().toUpperCase();
  }

  @Override
  public String toString() {
    return toJSONString();
  }

  public String toJSONString() {
    return toJSON().toJSONString();
  }

  public ExtendedJSONObject toJSON() {
    ExtendedJSONObject o = new ExtendedJSONObject();

    o.put("uuid", uuid);
    o.put("name", name);

    o.put("type", type);
    o.put("layout", layout);

    o.put("apps", apps);

    if (addedAt > 0) {
      o.put("addedAt", addedAt);
    }
    if (modifiedAt > 0) {
      o.put("modifiedAt", modifiedAt);
    }

    return o;
  }

  public static DeviceRecord fromJSONObject(ExtendedJSONObject o) throws NonObjectJSONException {
    String uuid = o.getString("uuid");
    String name = o.getString("name");

    String type = o.getString("type");
    String layout = o.getString("layout");

    long addedAt = o.getLong("addedAt").longValue();
    long modifiedAt = o.getLong("modifiedAt").longValue();

    ExtendedJSONObject apps = o.getObject("apps");

    return new DeviceRecord(uuid, name, type, layout, apps, addedAt, modifiedAt);
  }
}