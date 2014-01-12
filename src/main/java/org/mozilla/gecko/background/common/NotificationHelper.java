/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.common;

import org.mozilla.gecko.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class NotificationHelper {
  public static void displayNotification(final Context context,
                                         final int id,
                                         final String title,
                                         final String description,
                                         final String uri) {
    displayNotification(context, id, title, description, uri, R.drawable.icon, System.currentTimeMillis());
  }

  public static void displayNotification(final Context context,
                                         final int id,
                                         int titleID,
                                         int descriptionID,
                                         String uri) {
    String title = context.getString(titleID);
    String description = context.getString(descriptionID);
    displayNotification(context, id, title, description, uri);
  }

  // Some Notification APIs were deprecated in API11, and replaced by new ones.
  // We still target API8, so suppress the warnings.
  @SuppressWarnings("deprecation")
  public static void displayNotification(final Context context,
                                         final int id,
                                         final String title,
                                         final String description,
                                         final String uri,
                                         final int icon,
                                         final long when) {
    final String ns = Context.NOTIFICATION_SERVICE;
    final NotificationManager notificationManager = (NotificationManager) context.getSystemService(ns);

    Notification notification = new Notification(icon, title, when);
    notification.flags = Notification.FLAG_AUTO_CANCEL;

    // Set pending intent associated with the notification.
    Intent notificationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
    PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
    notification.setLatestEventInfo(context, title, description, contentIntent);

    // Send notification.
    notificationManager.notify(id, notification);
  }
}
