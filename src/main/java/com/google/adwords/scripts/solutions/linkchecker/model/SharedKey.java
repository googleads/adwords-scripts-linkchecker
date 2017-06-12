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
import java.util.UUID;

/**
 * This class represents a shared key which is generated on AppEngine and then accessed via
 * Datastore API from the client.
 */
@Cache
@Entity
public class SharedKey {
  @Id private String id;
  private String key;
  
  public SharedKey() {}
  
  private SharedKey(String key) {
    this.id = "key";
    this.key = key;
  }
  
  public static SharedKey createRandom() {
    return new SharedKey(UUID.randomUUID().toString());
  }
  
  public String getKeyText() {
    return key;
  }
}
