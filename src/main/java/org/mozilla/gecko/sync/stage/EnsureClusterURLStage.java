/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Android Sync Client.
 *
 * The Initial Developer of the Original Code is
 * the Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *  Richard Newman <rnewman@mozilla.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.gecko.sync.stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.NodeAuthenticationException;
import org.mozilla.gecko.sync.NullClusterURLException;
import org.mozilla.gecko.sync.ThreadPool;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.SyncResourceDelegate;

import android.util.Log;
import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;

public class EnsureClusterURLStage implements GlobalSyncStage {
  public interface ClusterURLFetchDelegate {
    /**
     * 200 - Success.
     * @param url The node/weave cluster URL returned by the server.
     */
    public void handleSuccess(URI url);

    /**
     * 200 - Success, but the server returned 'null', meaning no node can be
     * assigned at this time, probably due to sign up throttling.
     */
    public void handleThrottled();

    /**
     * 404 - User not found.
     * 503 - Service unavailable.
     * @param response The server's response.
     */
    public void handleFailure(HttpResponse response);

    /**
     * An unexpected error occurred.
     *
     * @param e
     */
    public void handleError(Exception e);
  }

  protected static final String LOG_TAG = "EnsureClusterURLStage";

  // TODO: if cluster URL has changed since last time, we need to ensure that we do
  // a fresh start. This takes place at the GlobalSession level. Verify!
  /**
   * Fetch a node/weave cluster URL from a server.
   *
   * @param nodeWeaveURL
   *          Where to request the cluster URL from, usually something like:
   *          <code>https://server/pathname/version/username/node/weave</code>.
   * @param delegate
   * @throws URISyntaxException
   */
  public static void fetchClusterURL(final String nodeWeaveURL,
                                     final ClusterURLFetchDelegate delegate) throws URISyntaxException {
    Log.d(LOG_TAG, "In fetchClusterURL: node/weave is " + nodeWeaveURL);

    BaseResource resource = new BaseResource(nodeWeaveURL);
    resource.delegate = new SyncResourceDelegate(resource) {

      /**
       * Handle the response for GET https://server/pathname/version/username/node/weave.
       *
       * Returns the Weave (aka Sync) Node that the client is located on.
       * Sync-specific calls should be directed to that node.
       *
       * Return value: the node URL, an unadorned (not JSON) string.
       *
       * node may be 'null' if no node can be assigned at this time, probably
       * due to sign up throttling.
       *
       * Possible errors:
       *
       * 503: there was an error getting a node | empty body
       *
       * 400: for historical reasons treated as 404.
       *
       * 404: user not found | empty body
       *
       * {@link http://docs.services.mozilla.com/reg/apis.html}
       */
      @Override
      public void handleHttpResponse(HttpResponse response) {

        int status = response.getStatusLine().getStatusCode();
        switch (status) {
        case 200:
          Log.i(LOG_TAG, "Got 200 for node/weave cluster URL request (user found; succeeding).");
          HttpEntity entity = response.getEntity();
          if (entity == null) {
            delegate.handleThrottled();
            return;
          }
          String output = null;
          try {
            InputStream content = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(content, "UTF-8"), 1024);
            output = reader.readLine();
            SyncResourceDelegate.consumeReader(reader);
            reader.close();
          } catch (IllegalStateException e) {
            delegate.handleError(e);
            return;
          } catch (IOException e) {
            delegate.handleError(e);
            return;
          }

          if (output == null || output.equals("null")) {
            delegate.handleThrottled();
          } else {
            try {
              URI uri = new URI(output);
              delegate.handleSuccess(uri);
            } catch (URISyntaxException e) {
              delegate.handleError(e);
            }
          }
          break;
        case 400:
        case 404:
          Log.i(LOG_TAG, "Got " + status + " for node/weave cluster URL request (user not found; failing).");
          delegate.handleFailure(response);
          SyncResourceDelegate.consumeEntity(response.getEntity());
          break;
        case 503:
          Log.i(LOG_TAG, "Got 503 for node/weave cluster URL request (error fetching node; failing).");
          delegate.handleFailure(response);
          SyncResourceDelegate.consumeEntity(response.getEntity());
          break;
        default:
          Log.w(LOG_TAG, "Got " + status + " for node/weave cluster URL request (unexpected HTTP status; failing).");
          delegate.handleFailure(response);
          SyncResourceDelegate.consumeEntity(response.getEntity());
        }
      }

      @Override
      public void handleHttpProtocolException(ClientProtocolException e) {
        delegate.handleError(e);
      }

      @Override
      public void handleHttpIOException(IOException e) {
        delegate.handleError(e);
      }

      @Override
      public void handleTransportException(GeneralSecurityException e) {
        delegate.handleError(e);
      }
    };

    resource.get();
  }

  public void execute(final GlobalSession session) throws NoSuchStageException {
    final URI oldClusterURL = session.config.getClusterURL();
    final boolean wantNodeAssignment = session.callback.wantNodeAssignment();

    if (!wantNodeAssignment && oldClusterURL != null) {
      Log.i(LOG_TAG, "Cluster URL is already set and not stale. Continuing with sync.");
      session.advance();
      return;
    }

    Log.i(LOG_TAG, "Fetching cluster URL.");
    final ClusterURLFetchDelegate delegate = new ClusterURLFetchDelegate() {

      @Override
      public void handleSuccess(final URI url) {
        Log.i(LOG_TAG, "Node assignment pointed us to " + url);

        if (oldClusterURL != null && oldClusterURL.equals(url)) { // equalsIgnoreCase?
          // Our cluster URL is marked as stale and the fresh cluster URL is the same.  Problem between keyboard and monitor.
          session.callback.informNodeAuthenticationFailed(session, url);
          session.abort(new NodeAuthenticationException(), "User password has changed.");
          return;
        }

        session.callback.informNodeAssigned(session, oldClusterURL, url); // No matter what, we're getting a new node/weave clusterURL.
        session.config.setClusterURL(url);

        ThreadPool.run(new Runnable() {
          @Override
          public void run() {
            session.advance();
          }
        });
      }

      @Override
      public void handleThrottled() {
        session.abort(new NullClusterURLException(), "Got 'null' cluster URL. Aborting.");
      }

      @Override
      public void handleFailure(HttpResponse response) {
        int statusCode = response.getStatusLine().getStatusCode();
        Log.w(LOG_TAG, "Got HTTP failure fetching node assignment: " + statusCode);
        if (statusCode == 404) {
          URI serverURL = session.config.serverURL;
          if (serverURL != null) {
            Log.i(LOG_TAG, "Using serverURL <" + serverURL.toASCIIString() + "> as clusterURL.");
            session.config.setClusterURL(serverURL);
            session.advance();
            return;
          }
          Log.w(LOG_TAG, "No serverURL set to use as fallback cluster URL. Aborting sync.");
          // Fallthrough to abort.
        } else {
          session.interpretHTTPFailure(response);
        }
        session.abort(new Exception("HTTP failure."), "Got failure fetching cluster URL.");
      }

      @Override
      public void handleError(Exception e) {
        session.abort(e, "Got exception fetching cluster URL.");
      }
    };

    ThreadPool.run(new Runnable() {
      @Override
      public void run() {
        try {
          fetchClusterURL(session.config.nodeWeaveURL(), delegate);
        } catch (URISyntaxException e) {
          session.abort(e, "Invalid URL for node/weave.");
        }
      }});
  }
}
