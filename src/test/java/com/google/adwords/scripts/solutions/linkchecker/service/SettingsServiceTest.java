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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.adwords.scripts.solutions.linkchecker.datastore.Datastore;
import com.google.adwords.scripts.solutions.linkchecker.model.Settings;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/** Tests for the SettingsService class. */
public class SettingsServiceTest {
  @Test
  public void getSettingsTest() {
    Datastore datastore = mock(Datastore.class);
    SettingsService settingsService = new SettingsService(datastore);

    settingsService.getSettings();

    verify(datastore, times(1)).getSettings();
    verify(datastore, times(1)).createDefaultSettings();
  }

  @Test
  public void updateSettingsTest()
      throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException {
    Settings settings = new Settings(120, Settings.DEFAULT_USER_AGENT);
    Datastore datastore = mock(Datastore.class);
    when(datastore.getSettings()).thenReturn(settings);
    SettingsService settingsService = new SettingsService(datastore);

    ArgumentCaptor<Settings> arg1 = ArgumentCaptor.forClass(Settings.class);
    settingsService.updateSettings(settings);

    verify(datastore).updateSettings(arg1.capture());
    Settings actualSettings = arg1.getValue();

    assertEquals(120, actualSettings.getRateInChecksPerMinute());
    assertEquals(Settings.DEFAULT_USER_AGENT, actualSettings.getUserAgentString());
  }
}
