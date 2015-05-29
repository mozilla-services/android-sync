/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.net;

import javax.net.ssl.SSLContext;

import org.mozilla.gecko.background.common.GlobalConstants;

import ch.boye.httpclientandroidlib.conn.ssl.SSLConnectionSocketFactory;

public class TLSSocketFactory extends SSLConnectionSocketFactory {

  public TLSSocketFactory(SSLContext sslContext) {
    super(sslContext, GlobalConstants.DEFAULT_PROTOCOLS, GlobalConstants.DEFAULT_CIPHER_SUITES, SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
  }
}
