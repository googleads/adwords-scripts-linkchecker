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

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.Parent;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Class to represent a batch of URLs uploaded for checking. Each batch is split into smaller units
 * internally, but for interactions with the client, the {@code BatchOperation} is the unit of
 * processing.
 */
@Cache
@Entity
public class BatchOperation {
  @Id private String batchId;
  private BatchOperationStatus status;
  
  // Created is indexed, to allow searching for {@code BatchOperation}s before a given date.
  @Index private Date created;

  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
  @Load
  @Parent
  Key<Account> accountId;

  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
  List<String> failureMatchTexts;
  
  // A count is kept of the remaining sub operations. Each sub operation updates this value when it
  // completes, and when this value reaches 0, the status is marked as complete.
  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
  private int remainingSubOperations;

  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
  @Load(BatchSubOperation.class)
  List<Ref<BatchSubOperation>> subOperations;

  public BatchOperation() {}

  public BatchOperation(String accountId, List<String> failureMatchTexts) {
    this.accountId = Key.create(Account.class, accountId);
    batchId = UUID.randomUUID().toString();
    status = BatchOperationStatus.COMPLETE;
    subOperations = new ArrayList<>();
    created = new Date();
    remainingSubOperations = 0;
    this.failureMatchTexts = failureMatchTexts;
  };

  /**
   * Adds a list of {@code BatchSubOperation}s to the {@code BatchOperation}.
   *
   * @param subOps 
   */
  public void addSubOperations(List<BatchSubOperation> subOps) {
    for (BatchSubOperation subOp : subOps) {
      subOperations.add(Ref.create(subOp));
    }
    remainingSubOperations += subOps.size();
    if (remainingSubOperations > 0) {
      status = BatchOperationStatus.PROCESSING;
    }
  }

  public String getBatchId() {
    return batchId;
  }

  public BatchOperationStatus getStatus() {
    return status;
  }

  public Date getCreatedDate() {
    return created;
  }

  public String getAccountId() {
    return accountId.getName();
  }

  public List<BatchSubOperation> getSubOperations() {
    List<BatchSubOperation> response = null;
    if (subOperations != null) {
      response = new ArrayList<>();
      for (Ref<BatchSubOperation> subOp : subOperations) {
        response.add(subOp.getValue());
      }
    }
    return response;
  }
  
  public List<String> getFailureMatchTexts() {
    return failureMatchTexts;
  }

  /**
   * Decrements the count of remaining subOperations, and if there are none left, marks the 
   * {@code BatchOperation} as complete.
   */
  public void decrementRemainingSubOperations() {
    if (remainingSubOperations > 0) {
      remainingSubOperations--;
      if (remainingSubOperations == 0) {
        status = BatchOperationStatus.COMPLETE;
      }
    }
  }
}
