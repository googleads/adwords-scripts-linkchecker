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
import com.google.adwords.scripts.solutions.linkchecker.model.BatchOperation;
import com.google.adwords.scripts.solutions.linkchecker.model.BatchSubOperation;
import com.google.adwords.scripts.solutions.linkchecker.request.UrlCheckRequest;
import com.google.adwords.scripts.solutions.linkchecker.response.BatchOperationDetailResponse;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Provides the means for creating and manipulating "batches" of URLs to check the status of.
 */
public class BatchOperationService {
  // Limit the number of URLs to be processed in one unit. This is to limit the size of the request
  // and response from the service. Large lists of URLs should be submitted as multiple operations.
  public static final int MAX_BATCH_URLS = 15000;

  private final Datastore datastore;
  private final TaskService taskService;

  @Inject
  public BatchOperationService(Datastore datastore, TaskService taskService) {
    this.datastore = datastore;
    this.taskService = taskService;
  }

  /**
   * Creates a new {@code BatchOperation}, any associated {@code BatchSubOperation}s, adds to the
   * Datastore and creates the necessary TaskQueue tasks to initiate processing.
   *
   * @param accountId The associated account ID.
   * @param request The object representing the list of URLs to check.
   * @return The ID of the created job.
   * @throws InterruptedException 
   */
  public String createNewBatchOperation(String accountId, UrlCheckRequest request)
      throws InterruptedException {
    // Deduplicate the list of URLs
    List<String> dedupedUrls = Lists.newArrayList(Sets.newHashSet(request.getUrls()));
    if (dedupedUrls.size() > MAX_BATCH_URLS) {
      throw new IllegalArgumentException("Too many URLs supplied");
    }

    BatchOperation operation = new BatchOperation(accountId, request.getFailureMatchTexts());
    List<BatchSubOperation> subOperations = new ArrayList<>();
    for (List<String> urls : Lists.partition(dedupedUrls, BatchSubOperation.MAX_URLS)) {
      BatchSubOperation subOp = new BatchSubOperation(operation, urls);
      subOperations.add(subOp);
    }
    operation.addSubOperations(subOperations);

    // DataStore has the lower quota when compared to TaskQueue, so the save operation will fail
    // and throw an OverQuota exception if there is no DataStore write quota available.
    datastore.saveBatchOperationAndChildren(operation, subOperations);
   
    taskService.createTasksForBatchSubOperations(accountId, subOperations);

    return operation.getBatchId();
  }

  /**
   * Retrieves details of a {@code BatchOperation} state.
   *
   * @param accountId The associated account ID.
   * @param id The ID of the BatchOperation to retrieve
   * @return The details as a BatchOperationDetailResponse object
   */
  public BatchOperationDetailResponse getOperationById(String accountId, String id) {
    BatchOperation op = datastore.loadBatchOperation(accountId, id);
    return BatchOperationDetailResponse.fromBatchOperation(op);
  }

  /**
   * Lists the current {@code BatchOperation}s for a given account.
   *
   * @param accountId The Account ID.
   * @return A list of {@code BatchOperation}s, their states and IDs.
   */
  public List<BatchOperation> listBatchOperations(String accountId) {
    return datastore.listBatchOperations(accountId);
  }

  /**
   * Lists {@code BatchOperation}s for an account that were created before a specified date.
   *
   * @param cutoffDate The upper-bound (exclusive) for {@code BatchOperation} created dates.
   * @return A list of {@code BatchOperation}s.
   */
  public List<BatchOperation> listHistoricBatchOperations(Date cutoffDate) {
    return datastore.listHistoricBatchOperations(cutoffDate);
  }

  /**
   * Deletes a specified {@code BatchOperation}.
   *
   * @param accountId The associated account ID.
   * @param id The ID of the {@code BatchOperation} to delete.
   */
  public void deleteBatchOperation(String accountId, String id) {
    datastore.deleteBatchOperation(accountId, id);
  }
}
