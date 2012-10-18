package org.mozilla.gecko.aitc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.apache.commons.codec.binary.Base64;
import org.mozilla.apache.commons.codec.digest.DigestUtils;
import org.mozilla.gecko.aitc.net.BlockingAppsClient;
import org.mozilla.gecko.browserid.mockmyid.MockMyIDTokenFactory;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.tokenserver.BlockingTokenServerClient;
import org.mozilla.gecko.tokenserver.TokenServerToken;

public class TestRemoteAppsClient {
  public static final String TEST_USERNAME = "testx";
  public static final String TEST_AUDIENCE = "https://myapps.mozillalabs.com"; // Default audience accepted by a local dev token server.
  public static final String TEST_TOKEN_SERVER_URL = "https://stage-token.services.mozilla.com";
  public static final String TEST_URL = TEST_TOKEN_SERVER_URL + "/1.0/aitc/1.0";

  protected final String assertion;
  protected final TokenServerToken token;

  protected BlockingAppsClient client;

  public TestRemoteAppsClient() throws Exception {
    assertion = new MockMyIDTokenFactory().createMockMyIDAssertion(TEST_USERNAME, TEST_AUDIENCE);

    BlockingTokenServerClient tokenServerClient = new BlockingTokenServerClient(new URI(TEST_URL));
    token = tokenServerClient.getTokenFromBrowserIDAssertion(assertion, false);
  }

  @Before
  public void setUp() throws Exception {
    client = new BlockingAppsClient(token);
  }

  @Test
  public void testAppId() {
    AppRecord app = new AppRecord("Examplinator 3000", "https://example.com", "/examplinator.webapp", "https://marketplace.mozilla.org");

    assertEquals(Base64.encodeBase64URLSafeString(DigestUtils.sha(app.origin)), app.appId());
  }

  @Test
  public void testPutDeleteApp() throws Exception {
    AppRecord app = new AppRecord("Examplinator 3000", "https://example.com", "/examplinator.webapp", "https://marketplace.mozilla.org");
    String appId = app.appId();

    client.putApp(app);

    Map<String, AppRecord> beforeApps = client.getApps(0, false);
    assertTrue(beforeApps.containsKey(appId));

    client.deleteApp(app.appId());

    Map<String, AppRecord> afterApps = client.getApps(0, false);
    assertFalse(afterApps.containsKey(appId));
  }

  @Test
  public void testGetApps() throws Exception {
    AppRecord app = new AppRecord("Examplinator 3000", "https://example.com", "/examplinator.webapp", "https://marketplace.mozilla.org");
    String appId = app.appId();

    client.putApp(app);

    AppRecord abbreviated = client.getApps(0, false).get(appId);

    // Abbreviated record does not include some fields, for example name.
    assertEquals(app.origin, abbreviated.origin);
    assertNull(abbreviated.name);
    assertNull(abbreviated.receipts);

    AppRecord full = client.getApps(0, true).get(appId);

    // Full record includes all fields.
    assertEquals(app.origin, full.origin);
    assertEquals(app.name, full.name);
    assertEquals(app.receipts, full.receipts);

    assertFalse(client.getApps(full.modifiedAt, false).containsKey(appId));
    assertTrue(client.getApps(full.modifiedAt - 1, false).containsKey(appId));
  }

  @Test
  public void testUUID() {
    String uuid = DeviceRecord.generateUUID();
    assertEquals(uuid.toUpperCase(), uuid);

    String parts[] = uuid.split("-");

    assertEquals(5, parts.length);
    assertEquals(8, parts[0].length());
    assertEquals(4, parts[1].length());
    assertEquals(4, parts[2].length());
    assertEquals(4, parts[3].length());
    assertEquals(12, parts[4].length());
  }

  @Test
  public void testPutDeleteDevice() throws Exception {
    DeviceRecord device = new DeviceRecord(DeviceRecord.generateUUID(), "Anant's Mac Pro", "mobile", "android/phone", new ExtendedJSONObject());
    client.putDevice(device);

    Map<String, DeviceRecord> beforeDevices = client.getDevices(0, false);
    assertTrue(beforeDevices.containsKey(device.uuid));

    client.deleteDevice(device.uuid);

    Map<String, DeviceRecord> afterDevices = client.getDevices(0, false);
    assertFalse(afterDevices.containsKey(device.uuid));
  }

  @Test
  public void testGetDevices() throws Exception {
    ExtendedJSONObject apps = new ExtendedJSONObject();
    apps.put("key", "value");

    DeviceRecord device = new DeviceRecord(DeviceRecord.generateUUID(), "Anant's Mac Pro", "mobile", "android/phone", apps);
    client.putDevice(device);

    DeviceRecord abbreviated = client.getDevices(0, false).get(device.uuid);

    // Abbreviated record does not include the apps field.
    assertEquals(device.name, abbreviated.name);
    assertNull(abbreviated.apps);

    DeviceRecord full = client.getDevices(0, true).get(device.uuid);

    // Full record includes all fields.
    assertEquals(device.name, full.name);
    assertEquals(device.apps.toJSONString(), full.apps.toJSONString());

    assertFalse(client.getDevices(full.modifiedAt, false).containsKey(device.uuid));
    assertTrue(client.getDevices(full.modifiedAt - 1, false).containsKey(device.uuid));
  }

  @Test
  public void testGetDevice() throws Exception {
    ExtendedJSONObject apps = new ExtendedJSONObject();
    apps.put("key", "value");

    DeviceRecord device = new DeviceRecord(DeviceRecord.generateUUID(), "Anant's Mac Pro", "mobile", "android/phone", apps);
    client.putDevice(device);

    DeviceRecord full = client.getDevice(device.uuid);

    // Full record includes all fields.
    assertEquals(device.name, full.name);
    assertEquals(device.apps.toJSONString(), full.apps.toJSONString());

    // XXX later assertNull(client.getDevice(DeviceRecord.generateUUID()));
  }
}
