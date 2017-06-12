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

import com.google.adwords.scripts.solutions.linkchecker.urlcheck.UrlCheckStatus;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.Parent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Class to represent a sub operation. This unit of work is taken on by a single TaskQueue entry.
 */
@Cache
@Entity
public class BatchSubOperation {
  public static final int MAX_URLS = 100;
  
  @Id private String id;
  private List<UrlCheckStatus> urlStatuses;
  
  @Parent
  @Load private Ref<BatchOperation> parentOp;
  
  public BatchSubOperation() {
    id = UUID.randomUUID().toString();
  }
  
  public BatchSubOperation(BatchOperation parentOp, List<String> urls) {
    this();
    if (urls.size() > MAX_URLS) {
      throw new IllegalArgumentException("Too many URLs supplied");
    }
    this.parentOp = Ref.create(parentOp);
    urlStatuses = new ArrayList<>();
    
    for (String url : urls) {
      urlStatuses.add(UrlCheckStatus.fromUrl(url));
    }
  }
  
  public String getId() {
    return id;
  }
  
  public List<UrlCheckStatus> getUrlStatuses() {
    return urlStatuses;
  }
  
  public BatchOperation getParent() {
    return parentOp.getValue();
  }
}
