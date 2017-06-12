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

package com.google.adwords.scripts.solutions.linkchecker.response;

import com.google.adwords.scripts.solutions.linkchecker.model.BatchOperation;
import com.google.adwords.scripts.solutions.linkchecker.model.BatchOperationStatus;
import com.google.adwords.scripts.solutions.linkchecker.model.BatchSubOperation;
import com.google.adwords.scripts.solutions.linkchecker.urlcheck.UrlCheckStatus;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to format a {@code BatchOperation} and all child {@code BatchSubOperation}s in a digestible
 * response. Any detail of sub operations is abstracted away, and all details are omitted when the
 * {@code BatchOperation} is still processing.
 */
public class BatchOperationDetailResponse {
  private final List<UrlCheckStatus> errors;
  private final BatchOperationStatus status;
  private final String batchId;
  
  private final int checkedUrlCount;
  
  private BatchOperationDetailResponse(BatchOperation op) {
    errors = new ArrayList<>(); 
    status = op.getStatus();
    batchId = op.getBatchId();
    
    int count = 0;
    if (status == BatchOperationStatus.COMPLETE) {
      List<BatchSubOperation> subOps = op.getSubOperations();
      for (BatchSubOperation subOp : subOps) {
        List<UrlCheckStatus> urlResults = subOp.getUrlStatuses();
        for (UrlCheckStatus urlResult : urlResults) {
          if (urlResult.getStatus() == UrlCheckStatus.Status.FAILURE) {
            errors.add(urlResult);
          }
        }
        count += urlResults.size();
      }
    }
    checkedUrlCount = count;
  }
  
  /**
   * Creates a {@code BatchOperationDetailResponse} representing the state of a given
   * {@code BatchOperation}.
   *
   * @param op The {@code BatchOperation} to represent.
   * @return The created {@code BatchOperationDetailResponse} object.
   */
  public static BatchOperationDetailResponse fromBatchOperation(BatchOperation op) {
    return new BatchOperationDetailResponse(op);
  }
  
  public String getBatchId() {
    return batchId;
  }
  
  public BatchOperationStatus getStatus() {
    return status;
  }
  
  public List<UrlCheckStatus> getErrors() {
    return errors;
  }
  
  public int getCheckedUrlCount() {
    return checkedUrlCount;
  }
}
