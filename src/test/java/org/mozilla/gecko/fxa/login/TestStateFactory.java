/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.fxa.login;

import junit.framework.Assert;

import org.junit.Test;
import org.mozilla.gecko.browserid.BrowserIDKeyPair;
import org.mozilla.gecko.browserid.RSACryptoImplementation;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Utils;

public class TestStateFactory {
  @Test
  public void testGetStateV1() throws Exception {
    byte[] sessionToken = Utils.generateRandomBytes(32);
    byte[] kA = Utils.generateRandomBytes(32);
    byte[] kB = Utils.generateRandomBytes(32);
    BrowserIDKeyPair keyPair = RSACryptoImplementation.generateKeyPair(512);
    String certificate = "certificate";
    Married state1 = new Married("email", "uid", sessionToken, kA, kB, keyPair, certificate);
    ExtendedJSONObject o = state1.toJSONObject();
    State state2 = StateFactory.fromJSONObject(state1.stateLabel, o);
    Assert.assertEquals(state1.stateLabel, state2.stateLabel);
    Assert.assertEquals(state1.toJSONObject(), state2.toJSONObject());
  }
}
