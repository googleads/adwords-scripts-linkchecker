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
import com.google.adwords.scripts.solutions.linkchecker.model.SharedKey;
import com.google.inject.Inject;
import com.googlecode.objectify.NotFoundException;
 
/**
 * Provides methods for creating/retrieving a key in/from the Datastore for use in shared key
 * -authorized communications.
 */
public class SharedKeyService {
  Datastore datastore;
  
  @Inject
  public SharedKeyService(Datastore datastore) {
    this.datastore = datastore;
  }
  
  public SharedKey getKey() {
    SharedKey key;
    try {
      key = datastore.getKey();
      if (key == null) {
        key = datastore.createKey();
      }
    } catch (NotFoundException e) {
      key = datastore.createKey();
    }
    return key;
  }
}
