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

package com.google.adwords.scripts.solutions.linkchecker.model;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

/**
 * This class represents a settings which are generated on AppEngine and then modified via Datastore
 * API from the client.
 */
@Cache
@Entity
public class Settings {
  public static final int DEFAULT_CHECKS_PER_MINUTE = 60;
  public static final String DEFAULT_USER_AGENT = "GAE Link Checker";

  @Id private String id;
  private Integer rateInChecksPerMinute;
  private String userAgentString;

  public Settings() {
    this.id = "settings";
  }

  public Settings(int rateInChecksPerMinute, String userAgent) {
    this();
    this.rateInChecksPerMinute = rateInChecksPerMinute;
    this.userAgentString = userAgent;
  }

  public int getRateInChecksPerMinute() {
    return rateInChecksPerMinute;
  }

  public String getUserAgentString() {
    return userAgentString;
  }

  public static Settings createDefaultSettings() {
    return new Settings(DEFAULT_CHECKS_PER_MINUTE, DEFAULT_USER_AGENT);
  }
}
