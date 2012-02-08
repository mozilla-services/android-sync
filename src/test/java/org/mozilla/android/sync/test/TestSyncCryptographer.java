/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.mozilla.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.cryptographer.CryptoStatusBundle;
import org.mozilla.gecko.sync.cryptographer.CryptoStatusBundle.CryptoStatus;
import org.mozilla.gecko.sync.cryptographer.SyncCryptographer;

public class TestSyncCryptographer {
}
