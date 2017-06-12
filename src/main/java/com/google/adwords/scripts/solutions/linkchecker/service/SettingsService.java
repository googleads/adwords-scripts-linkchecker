// Copyright 2017, Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.adwords.scripts.solutions.linkchecker.service;

import com.google.adwords.scripts.solutions.linkchecker.datastore.Datastore;
import com.google.adwords.scripts.solutions.linkchecker.model.Settings;
import com.google.inject.Inject;
import com.googlecode.objectify.NotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides methods for creating, modifying and retrieving user-modifiable settings from Datastore.
 */
public class SettingsService {
  private static final Logger LOG = Logger.getLogger(SettingsService.class.getName());
  private final Datastore datastore;

  @Inject
  public SettingsService(Datastore datastore) {
    this.datastore = datastore;
  }

  public Settings getSettings() {
    Settings settings;
    try {
      settings = datastore.getSettings();
      if (settings == null) {
        settings = datastore.createDefaultSettings();
      }
    } catch (NotFoundException e) {
      settings = datastore.createDefaultSettings();
    }
    return settings;
  }

  /**
   * Updates the settings in Datastore with the settings provides via the REST call. Only properties
   * with non-null values are updated, allowing the caller to send partial settings updates, as if
   * "PATCH" were supported by GAE.
   *
   * @param update
   * @return
   */
  public Settings updateSettings(Settings update) {
    Settings current = getSettings();

    for (Field field : update.getClass().getDeclaredFields()) {
      field.setAccessible(true);
      if (!Modifier.isFinal(field.getModifiers())) {
        try {
          if (field.get(update) != null) {
            Field updateField = current.getClass().getDeclaredField(field.getName());
            updateField.setAccessible(true);
            updateField.set(current, field.get(update));
          }
        } catch (IllegalAccessException | NoSuchFieldException e) {
          // If the partial update contains a field that is not in the definition of Settings, a
          // NoSuchFieldException will be thrown.
          LOG.log(Level.WARNING, "Settings update contained unknown property {0}", field.getName());
        }
      }
    }
    return datastore.updateSettings(current);
  }
}
