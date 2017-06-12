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
import com.google.inject.Inject;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Provides the means to periodically delete {@code BatchOperation}s over a given age, to avoid the
 * Datastore becoming clogged up.
 */
public class JobsCleanupService {
  public static final int OLD_JOB_CUTOFF_DAYS = 30;

  private final BatchOperationService batchOperationService;
  private final Datastore datastore;

  public static final String INDEX_BUILD_PATH = "/cron/jobscleanup";

  @Inject
  public JobsCleanupService(
      BatchOperationService batchOperationService, Datastore datastore) {
    this.batchOperationService = batchOperationService;
    this.datastore = datastore;
  }

  /**
   * Deletes {@code BatchOperation}s that were created before then cutoff number of days ago.
   */
  public void cleanup() {
    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    cal.add(Calendar.DATE, -OLD_JOB_CUTOFF_DAYS);
    Date cutoffDate = cal.getTime();

    List<BatchOperation> oldOps = batchOperationService.listHistoricBatchOperations(cutoffDate);
    for (BatchOperation oldOp : oldOps) {
      datastore.deleteBatchOperation(oldOp.getAccountId(), oldOp.getBatchId());
    }
  }
}
