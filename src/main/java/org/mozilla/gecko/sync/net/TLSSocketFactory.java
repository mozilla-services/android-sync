/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import org.mozilla.gecko.background.common.GlobalConstants;
import org.mozilla.gecko.background.common.log.Logger;

import ch.boye.httpclientandroidlib.HttpHost;
import ch.boye.httpclientandroidlib.conn.ConnectTimeoutException;
import ch.boye.httpclientandroidlib.conn.ssl.SSLSocketFactory;
import ch.boye.httpclientandroidlib.params.HttpParams;
import ch.boye.httpclientandroidlib.protocol.HttpContext;

public class TLSSocketFactory extends SSLSocketFactory {
  private static final String LOG_TAG = "TLSSocketFactory";

  // Guarded by `this`.
  private static String[] cipherSuites = GlobalConstants.DEFAULT_CIPHER_SUITES;

  public TLSSocketFactory(SSLContext sslContext) {
    super(sslContext, GlobalConstants.DEFAULT_PROTOCOLS, GlobalConstants.DEFAULT_CIPHER_SUITES, null);
  }

  /**
   * Attempt to specify the cipher suites to use for a connection. If
   * setting fails (as it will on Android 2.2, because the wrong names
   * are in use to specify ciphers), attempt to set the defaults.
   *
   * We store the list of cipher suites in `cipherSuites`, which
   * avoids this fallback handling having to be executed more than once.
   *
   * This method is synchronized to ensure correct use of that member.
   *
   * See Bug 717691 for more details.
   *
   * @param socket
   *        The SSLSocket on which to operate.
   */
  public static synchronized void setEnabledCipherSuites(SSLSocket socket) {
    try {
      socket.setEnabledCipherSuites(cipherSuites);
    } catch (IllegalArgumentException e) {
      cipherSuites = socket.getSupportedCipherSuites();
      Logger.warn(LOG_TAG, "Setting enabled cipher suites failed: " + e.getMessage());
      Logger.warn(LOG_TAG, "Using " + cipherSuites.length + " supported suites.");
      socket.setEnabledCipherSuites(cipherSuites);
    }
  }

  private static void logSocket(Socket socket) {
    if (socket instanceof SSLSocket) {
      Logger.info(LOG_TAG, "XXX: Protocols: ");
      for (String protocol: ((SSLSocket) socket).getEnabledProtocols()) {
        Logger.info(LOG_TAG, "XXX:   --- " + protocol);
      }
      Logger.info(LOG_TAG, "XXX: Cipher suites: ");
      for (String suite: ((SSLSocket) socket).getEnabledCipherSuites()) {
        Logger.info(LOG_TAG, "XXX:   --- " + suite);
      }
    }
  }
  private static Socket adjustSocket(SSLSocket socket) {
    socket.setEnabledProtocols(GlobalConstants.DEFAULT_PROTOCOLS);
    //setEnabledCipherSuites(socket);
    logSocket(socket);
    return socket;
  }

  @Override
  public Socket createSocket() throws IOException {
    Logger.info(LOG_TAG, "XXX: Creating custom socket without params.");
    SSLSocket socket = (SSLSocket) super.createSocket();
    return adjustSocket(socket);
  }

  @Override
  public Socket createSocket(HttpParams params) throws IOException {
    Logger.info(LOG_TAG, "XXX: Creating custom socket.");
    SSLSocket socket = (SSLSocket) super.createSocket(params);
    return adjustSocket(socket);
  }

  @Override
  public Socket connectSocket(Socket socket, InetSocketAddress remoteAddress,
                              InetSocketAddress localAddress, HttpParams params)
                                                                                throws IOException,
                                                                                UnknownHostException,
                                                                                ConnectTimeoutException {
    Logger.info(LOG_TAG, "XXX connectSocket A");
    logSocket(socket);
    return super.connectSocket(socket, remoteAddress, localAddress, params);
  }

  @Override
  public Socket connectSocket(Socket socket, String host, int port,
                              InetAddress local, int localPort,
                              HttpParams params) throws IOException,
                                                UnknownHostException,
                                                ConnectTimeoutException {
    Logger.info(LOG_TAG, "XXX connectSocket B");
    logSocket(socket);
    return super.connectSocket(socket, host, port, local, localPort, params);
  }

  @Override
  public Socket connectSocket(int connectTimeout, Socket socket, HttpHost host,
                              InetSocketAddress remoteAddress,
                              InetSocketAddress localAddress,
                              HttpContext context) throws IOException {
    Logger.info(LOG_TAG, "XXX connectSocket C");
    logSocket(socket);
    return super.connectSocket(connectTimeout, socket, host, remoteAddress,
                               localAddress, context);
  }
}