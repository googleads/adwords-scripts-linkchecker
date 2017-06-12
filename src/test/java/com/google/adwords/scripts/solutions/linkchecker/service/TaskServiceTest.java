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

import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.Assert.assertEquals;

import com.google.adwords.scripts.solutions.linkchecker.model.BatchOperation;
import com.google.adwords.scripts.solutions.linkchecker.model.BatchSubOperation;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.dev.LocalTaskQueue;
import com.google.appengine.api.taskqueue.dev.QueueStateInfo;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Tests for the TaskService class. */
public class TaskServiceTest {
  private static final String TEST_ACCOUNT_ID = "123456";

  private final LocalTaskQueueTestConfig tqConfig = new LocalTaskQueueTestConfig();
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(tqConfig, new LocalDatastoreServiceTestConfig());
  private TaskService taskService;
  private Closeable closeable;

  @Before
  public void setUp() {
    tqConfig.setDisableAutoTaskExecution(true);
    helper.setUp();
    taskService = new TaskService();
    ObjectifyService.register(BatchOperation.class);
    ObjectifyService.register(BatchSubOperation.class);
    closeable = ObjectifyService.begin();
  }

  @After
  public void tearDown() {
    closeable.close();
    helper.tearDown();
  }

  @Test
  public void createTasksForBatchSubOperationsTest() throws InterruptedException {
    LocalTaskQueue ltq = LocalTaskQueueTestConfig.getLocalTaskQueue();
    BatchOperation op = new BatchOperation(TEST_ACCOUNT_ID, null);
    List<BatchSubOperation> subOps = Lists.newArrayList();
    // Add more tasks than can be queued up in one go, to enture TaskService
    // can add in bunches.
    for (int i = 0; i < TaskService.MAX_TASKS_ADD + 10; i++) {
      subOps.add(new BatchSubOperation(op, ImmutableList.of("http://www.example.com/" + i)));
    }
    op.addSubOperations(subOps);
    ofy().save().entity(op).now();
    ofy().save().entities(subOps).now();

    taskService.createTasksForBatchSubOperations(TEST_ACCOUNT_ID, subOps);

    // Allow time for tasks to add.
    Thread.sleep(1000);

    QueueStateInfo qsi = ltq.getQueueStateInfo().get(QueueFactory.getDefaultQueue().getQueueName());
    assertEquals(TaskService.MAX_TASKS_ADD + 10, qsi.getCountTasks());
  }
}
