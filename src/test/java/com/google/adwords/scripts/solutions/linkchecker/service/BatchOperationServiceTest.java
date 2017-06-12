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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.adwords.scripts.solutions.linkchecker.datastore.Datastore;
import com.google.adwords.scripts.solutions.linkchecker.model.BatchOperation;
import com.google.adwords.scripts.solutions.linkchecker.model.BatchSubOperation;
import com.google.adwords.scripts.solutions.linkchecker.request.UrlCheckRequest;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.Lists;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/** Tests for the BatchOperationService class. */
public class BatchOperationServiceTest {
  private static final String TEST_ACCOUNT_ID = "123456";

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private Closeable closeable;

  private Datastore datastore;
  private TaskService taskService;
  private BatchOperationService batchOperationService;

  @Before
  public void setUp() {
    helper.setUp();
    ObjectifyService.register(BatchOperation.class);
    ObjectifyService.register(BatchSubOperation.class);
    closeable = ObjectifyService.begin();
    datastore = mock(Datastore.class);
    taskService = mock(TaskService.class);
    batchOperationService = new BatchOperationService(datastore, taskService);
  }

  @After
  public void tearDown() {
    closeable.close();
    helper.tearDown();
  }

  @Test(expected = IllegalArgumentException.class)
  public void createNewBatchTooMany() throws InterruptedException {
    List<String> urls = Lists.newArrayList();
    // Exceed max urls by 1.
    for (int i = 0; i < BatchOperationService.MAX_BATCH_URLS + 1; i++) {
      urls.add("http://test" + i);
    }
    UrlCheckRequest request = new UrlCheckRequest(urls);
    batchOperationService.createNewBatchOperation(TEST_ACCOUNT_ID, request);
  }

  @Test
  public void createNewBatchTest() throws InterruptedException {
    List<BatchSubOperation> subOps = Lists.newArrayList();

    // The expected URL count, number of batches, and URLs in the last batch.
    // Test with slightly fewer than the maximum to make the last sub batch
    // contain not a full BatchSubOperation.MAX_URLS worth.
    int expectedUrlCount = BatchOperationService.MAX_BATCH_URLS - 25;
    int expectedSubBatchCount = expectedUrlCount / BatchSubOperation.MAX_URLS + 1;
    int expectedLastBatchCount = expectedUrlCount % BatchSubOperation.MAX_URLS;

    ArgumentCaptor<BatchOperation> arg1 = ArgumentCaptor.forClass(BatchOperation.class);
    ArgumentCaptor<? extends List> arg2 = ArgumentCaptor.forClass(subOps.getClass());

    List<String> urls = Lists.newArrayList();
    for (int i = 0; i < BatchOperationService.MAX_BATCH_URLS - 25; i++) {
      urls.add("http://test" + i);
    }
    UrlCheckRequest request = new UrlCheckRequest(urls);

    batchOperationService.createNewBatchOperation(TEST_ACCOUNT_ID, request);

    verify(datastore).saveBatchOperationAndChildren(arg1.capture(), arg2.capture());

    List<BatchSubOperation> actualSubOps = arg2.getValue();
    assertEquals(expectedSubBatchCount, actualSubOps.size());
    assertEquals(
        expectedLastBatchCount, actualSubOps.get(actualSubOps.size() - 1).getUrlStatuses().size());
    verify(taskService, times(1))
        .createTasksForBatchSubOperations(anyString(), isA(subOps.getClass()));
  }
}
