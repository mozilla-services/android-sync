/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync;
import java.net.URI;

import org.mozilla.gecko.sync.delegates.NodeAssignmentCallback;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class SharedPreferencesNodeAssignmentCallback implements NodeAssignmentCallback {
  protected final SharedPreferences sharedPreferences;

  public SharedPreferencesNodeAssignmentCallback(SharedPreferences sharedPreferences) {
    this.sharedPreferences = sharedPreferences;
  }

  public synchronized boolean getClusterURLIsStale() {
    return sharedPreferences.getBoolean(SyncConfiguration.PREF_CLUSTER_URL_IS_STALE, false);
  }

  public synchronized void setClusterURLIsStale(boolean clusterURLIsStale) {
    Editor edit = sharedPreferences.edit();
    edit.putBoolean(SyncConfiguration.PREF_CLUSTER_URL_IS_STALE, clusterURLIsStale);
    edit.commit();
  }

  @Override
  public boolean wantNodeAssignment() {
    return getClusterURLIsStale();
  }

  @Override
  public void informNodeAuthenticationFailed(GlobalSession session, URI failedClusterURL) {
    // TODO: communicate to the user interface that we need a new user password!
    // TODO: only freshen the cluster URL (better yet, forget the cluster URL) after the user has provided new credentials.
    setClusterURLIsStale(false);
  }

  @Override
  public void informNodeAssigned(GlobalSession session, URI oldClusterURL, URI newClusterURL) {
    setClusterURLIsStale(false);
  }
}
