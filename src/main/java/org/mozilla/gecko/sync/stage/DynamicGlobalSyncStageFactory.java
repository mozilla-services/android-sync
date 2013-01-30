/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.stage;

import java.lang.reflect.Constructor;
import java.util.Map;

import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;

/**
 * A global sync stage factory that
 * <ul>
 * <li>maintains a map from sync stage to global sync stage classes;</li>
 * <li>creates global sync stages dynamically by reflection.</li>
 * </ul>
 */
public class DynamicGlobalSyncStageFactory implements GlobalSyncStageFactory {
  public static final String LOG_TAG = DynamicGlobalSyncStageFactory.class.getSimpleName();

  public final Map<Stage, Class<? extends GlobalSyncStage>> klasses;

  public DynamicGlobalSyncStageFactory(Map<Stage, Class<? extends GlobalSyncStage>> klasses) {
    this.klasses = klasses;
  }

  @Override
  public GlobalSyncStage createGlobalSyncStage(Stage stage) {
    Class<? extends GlobalSyncStage> klass = klasses.get(stage);
    if (klass == null) {
      return null;
    }

    try {
      Constructor<? extends GlobalSyncStage> constructor = klass.getDeclaredConstructor();
      if (constructor == null) {
        return null;
      }
      return constructor.newInstance();
    } catch (Exception e) {
      Logger.warn(LOG_TAG, "Failed to dynamically create instance of " + klass + "; returning null.", e);
      return null;
    }
  }
}
