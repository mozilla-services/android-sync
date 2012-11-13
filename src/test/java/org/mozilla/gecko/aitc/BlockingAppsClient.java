package org.mozilla.gecko.aitc;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.aitc.net.AppsClient;
import org.mozilla.gecko.aitc.net.AppsClient.AppsClientDelegate;
import org.mozilla.gecko.aitc.net.AppsClient.AppsClientException;
import org.mozilla.gecko.aitc.net.AppsClient.AppsClientObjectDelegate;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonArrayJSONException;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.HMACAuthHeaderProvider;
import org.mozilla.gecko.tokenserver.TokenServerToken;

public class BlockingAppsClient {
  protected final AppsClient client;
  protected final TokenServerToken token;
  protected final AuthHeaderProvider authHeaderProvider;

  public BlockingAppsClient(TokenServerToken token) throws Exception {
    this.token = token;

    authHeaderProvider = new HMACAuthHeaderProvider(token.id, token.key);
    client = new AppsClient(new URI(token.endpoint), authHeaderProvider);
  }

  protected static class BlockingAppsClientDelegate implements AppsClientDelegate {
    @Override
    public void handleSuccess() {
      WaitHelper.getTestWaiter().performNotify();
    }

    @Override
    public void handleRemoteFailure(AppsClientException e) {
      WaitHelper.getTestWaiter().performNotify(e);
    }

    @Override
    public void handleError(Exception e) {
      WaitHelper.getTestWaiter().performNotify(e);
    }
  }

  protected static class BlockingObjectClientAppDelegate extends BlockingAppsClientDelegate implements AppsClientObjectDelegate {
    public final List<ExtendedJSONObject> objects = new ArrayList<ExtendedJSONObject>();

    @Override
    public void onObject(ExtendedJSONObject object) {
      objects.add(object);
    }
  }

  public Map<String, AppRecord> getApps(final long after, final boolean full) throws NonArrayJSONException {
    final BlockingObjectClientAppDelegate delegate = new BlockingObjectClientAppDelegate();

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.getApps(after, full, delegate);
      }
    });

    Map<String, AppRecord> apps = new LinkedHashMap<String, AppRecord>();
    for (ExtendedJSONObject object : delegate.objects) {
      AppRecord app = AppRecord.fromJSONObject(object);

      apps.put(app.appId(), app);
    }

    return apps;
  }

  public void putApp(final AppRecord app) {
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.putApp(app, new BlockingAppsClientDelegate());
      }
    });
  }

  protected void deleteApp(final String appId) {
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.deleteApp(appId, new BlockingAppsClientDelegate());
      }
    });
  }

  public Map<String, DeviceRecord> getDevices(final long after, final boolean full) throws NonObjectJSONException {
    final BlockingObjectClientAppDelegate delegate = new BlockingObjectClientAppDelegate();

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.getDevices(after, full, delegate);
      }
    });

    Map<String, DeviceRecord> devices = new LinkedHashMap<String, DeviceRecord>();
    for (ExtendedJSONObject object : delegate.objects) {
      DeviceRecord device = DeviceRecord.fromJSONObject(object);

      devices.put(device.uuid, device);
    }

    return devices;
  }

  public void putDevice(final DeviceRecord device) {
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.putDevice(device, new BlockingAppsClientDelegate());
      }
    });
  }

  protected void deleteDevice(final String uuid) {
    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.deleteDevice(uuid, new BlockingAppsClientDelegate());
      }
    });
  }

  public DeviceRecord getDevice(final String uuid) throws NonObjectJSONException {
    final BlockingObjectClientAppDelegate delegate = new BlockingObjectClientAppDelegate();

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        client.getDevice(uuid, delegate);
      }
    });

    if (delegate.objects.size() == 1) {
      for (ExtendedJSONObject object : delegate.objects) {
        return DeviceRecord.fromJSONObject(object);
      }
    }

    return null;
  }
}
