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

import com.google.adwords.scripts.solutions.linkchecker.model.BatchSubOperation;
import com.google.adwords.scripts.solutions.linkchecker.tasks.UrlCheckTask;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides the means for adding {@code BatchSubOperation}s to the TaskQueue.
 */
public class TaskService {
  public static final int MAX_TASKS_ADD = 100;
  
  public TaskService() {}
  
  /**
   * Adds a list of {@code BatchSubOperation}s to the TaskQueue. Performs this action asynchronously
   * as there may be quite a number to add.
   *
   * @param accountId The associated account ID.
   * @param subOps The list of {@code BatchSubOperation}s.
   */
  public void createTasksForBatchSubOperations(String accountId, List<BatchSubOperation> subOps) {
    List<TaskOptions> tasks = new ArrayList<>();
    Queue queue = QueueFactory.getDefaultQueue();
    
    for (BatchSubOperation subOp : subOps) {
      UrlCheckTask t =
          new UrlCheckTask(
              accountId, subOp.getParent().getBatchId(), subOp.getId());
      tasks.add(TaskOptions.Builder.withPayload(t));
    }
    
    List<List<TaskOptions>> partitions = Lists.partition(tasks, MAX_TASKS_ADD);
    for (List<TaskOptions> partition : partitions) {
      queue.addAsync(partition);
    }
  }
}
