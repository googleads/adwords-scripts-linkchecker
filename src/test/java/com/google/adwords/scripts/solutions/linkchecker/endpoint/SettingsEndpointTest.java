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

package com.google.adwords.scripts.solutions.linkchecker.endpoint;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.adwords.scripts.solutions.linkchecker.model.Settings;
import com.google.adwords.scripts.solutions.linkchecker.service.SettingsService;
import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Tests for the SettingsEndpoint class. */
public class SettingsEndpointTest {
  private final LocalServiceTestHelper testHelper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private SettingsService settingsService;
  private SettingsEndpoint settingsEndpoint;

  @Before
  public void setUp() throws Exception {
    testHelper.setUp();
    settingsService = mock(SettingsService.class);
    Settings settings = Settings.createDefaultSettings();
    when(settingsService.getSettings()).thenReturn(settings);
    settingsEndpoint = new SettingsEndpoint(settingsService);
  }

  @After
  public void tearDown() throws Exception {
    testHelper.tearDown();
  }

  @Test
  public void testGet() {
    Settings settings = settingsEndpoint.get();
    assertEquals(Settings.DEFAULT_CHECKS_PER_MINUTE, settings.getRateInChecksPerMinute());
    assertEquals(Settings.DEFAULT_USER_AGENT, settings.getUserAgentString());
  }

  @Test
  public void testPut() throws IllegalAccessException, NotFoundException, NoSuchFieldException {
    Settings settings = settingsEndpoint.get();
    settingsEndpoint.put(settings);
    verify(settingsService, times(1)).updateSettings(settings);
  }
}
