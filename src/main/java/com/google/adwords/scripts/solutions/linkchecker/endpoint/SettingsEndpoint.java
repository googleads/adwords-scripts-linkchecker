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

import com.google.adwords.scripts.solutions.linkchecker.annotation.Authorize;
import com.google.adwords.scripts.solutions.linkchecker.annotation.Authorize.Type;
import com.google.adwords.scripts.solutions.linkchecker.model.Settings;
import com.google.adwords.scripts.solutions.linkchecker.service.SettingsService;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.inject.Inject;

/** Defines v1 of a BatchLinkChecker API */
@Api(name = "batchLinkChecker", version = "v1", title = "Batch Link Checker API")
public class SettingsEndpoint {
  private final SettingsService settingsService;

  @Inject
  public SettingsEndpoint(SettingsService settingsService) {
    this.settingsService = settingsService;
  }
  /**
   * Retrieves the user-modifiable global settings.
   *
   * @return The settings object.
   */
  @Authorize(value = Type.SHARED_KEY)
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "settings")
  public Settings get() {
    return settingsService.getSettings();
  }

  @Authorize(value = Type.SHARED_KEY)
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.PUT, path = "settings")
  public Settings put(Settings settings) {
    return settingsService.updateSettings(settings);
  }
}
