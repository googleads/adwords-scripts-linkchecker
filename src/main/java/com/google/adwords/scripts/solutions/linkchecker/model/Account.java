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
 * Class representing an account. {@code BatchOperation}s are associated with a parent
 * {@code Account} object. This is to allow for the scenario where the AppEngine instance is used
 * from multiple AdWords Scripts accounts, and allows separation of the each associated list of
 * {@code BatchOperation} jobs.
 */
@Cache
@Entity
public class Account {
  @Id private String accountId;

  public Account() {}

  public Account(String accountId) {
    this.accountId = accountId;
  }

  public String getId() {
    return accountId;
  }
}
