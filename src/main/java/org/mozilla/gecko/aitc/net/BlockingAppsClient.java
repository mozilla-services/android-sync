package org.mozilla.gecko.aitc.net;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.mozilla.gecko.aitc.AppRecord;
import org.mozilla.gecko.aitc.DeviceRecord;
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
    public Exception exception = null;

    protected final CountDownLatch latch;

    public BlockingAppsClientDelegate(CountDownLatch latch) {
      this.latch = latch;
    }

    public BlockingAppsClientDelegate() {
      this(new CountDownLatch(1));
    }

    @Override
    public void handleSuccess() {
      latch.countDown();
    }

    @Override
    public void handleRemoteFailure(AppsClientException e) {
      this.exception = e;

      latch.countDown();
    }

    @Override
    public void handleError(Exception e) {
      this.exception = e;

      latch.countDown();
    }

    public void await() throws AppsClientException {
      try {
        latch.await();
      } catch (InterruptedException e) {
        throw new AppsClientException(e);
      }

      if (exception != null) {
        throw new AppsClientException(exception);
      }
    }
  }

  protected static class BlockingObjectClientAppDelegate extends BlockingAppsClientDelegate implements AppsClientObjectDelegate {
    public final List<ExtendedJSONObject> objects = new ArrayList<ExtendedJSONObject>();

    public BlockingObjectClientAppDelegate(CountDownLatch latch) {
      super(latch);
    }

    public BlockingObjectClientAppDelegate() {
      super();
    }

    @Override
    public void onObject(ExtendedJSONObject object) {
      objects.add(object);
    }
  }

  public Map<String, AppRecord> getApps(final long after, final boolean full) throws AppsClientException {
    final BlockingObjectClientAppDelegate delegate = new BlockingObjectClientAppDelegate();

    client.getApps(after, full, delegate);

    delegate.await();

    try {
      Map<String, AppRecord> apps = new LinkedHashMap<String, AppRecord>();
      for (ExtendedJSONObject object : delegate.objects) {
        AppRecord app = AppRecord.fromJSONObject(object);

        apps.put(app.appId(), app);
      }

      return apps;
    } catch (NonArrayJSONException e) {
      throw new AppsClientException(e);
    }
  }

  public void putApp(final AppRecord app) throws AppsClientException {
    CountDownLatch latch = new CountDownLatch(1);

    BlockingAppsClientDelegate delegate = new BlockingAppsClientDelegate(latch);
    client.putApp(app, delegate);

    delegate.await();
  }

  public void deleteApp(final String appId) throws AppsClientException {
    CountDownLatch latch = new CountDownLatch(1);

    BlockingAppsClientDelegate delegate = new BlockingAppsClientDelegate(latch);
    client.deleteApp(appId, delegate);

    delegate.await();
  }

  public Map<String, DeviceRecord> getDevices(final long after, final boolean full) throws AppsClientException {
    final BlockingObjectClientAppDelegate delegate = new BlockingObjectClientAppDelegate();

    client.getDevices(after, full, delegate);

    delegate.await();

    try {
      Map<String, DeviceRecord> devices = new LinkedHashMap<String, DeviceRecord>();
      for (ExtendedJSONObject object : delegate.objects) {
        DeviceRecord device = DeviceRecord.fromJSONObject(object);

        devices.put(device.uuid, device);
      }

      return devices;
    } catch (NonObjectJSONException e) {
      throw new AppsClientException(e);
    }
  }

  public void putDevice(final DeviceRecord device) throws AppsClientException {
    final BlockingObjectClientAppDelegate delegate = new BlockingObjectClientAppDelegate();

    client.putDevice(device, delegate);

    delegate.await();
  }

  public void deleteDevice(final String uuid) throws AppsClientException {
    final BlockingObjectClientAppDelegate delegate = new BlockingObjectClientAppDelegate();

    client.deleteDevice(uuid, delegate);

    delegate.await();
  }

  public DeviceRecord getDevice(final String uuid) throws AppsClientException {
    final BlockingObjectClientAppDelegate delegate = new BlockingObjectClientAppDelegate();

    client.getDevice(uuid, delegate);

    delegate.await();

    try {
      if (delegate.objects.size() == 1) {
        for (ExtendedJSONObject object : delegate.objects) {
          return DeviceRecord.fromJSONObject(object);
        }
      }
    } catch (NonObjectJSONException e) {
      throw new AppsClientException(e);
    }

    return null;
  }
}
