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

package com.google.adwords.scripts.solutions.linkchecker.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.adwords.scripts.solutions.linkchecker.model.BatchOperation;
import com.google.adwords.scripts.solutions.linkchecker.model.BatchOperationStatus;
import com.google.adwords.scripts.solutions.linkchecker.model.BatchSubOperation;
import com.google.adwords.scripts.solutions.linkchecker.model.Settings;
import com.google.adwords.scripts.solutions.linkchecker.model.SharedKey;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.NotFoundException;
import com.googlecode.objectify.ObjectifyService;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Tests for the Datastore class. */
public class DatastoreTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private com.googlecode.objectify.util.Closeable closeable;

  private static final String TEST_ACCOUNT_ID = "123456";
  private static final String TEST_ACCOUNT_ID2 = "098765";

  private Datastore datastore;

  public DatastoreTest() {}

  @Before
  public void setUp() {
    helper.setUp();
    closeable = ObjectifyService.begin();
    datastore = new Datastore();
  }

  @After
  public void tearDown() {
    closeable.close();
    helper.tearDown();
  }

  @Test
  public void testListBatchOperationsEmpty() {
    List<BatchOperation> operations = datastore.listBatchOperations(TEST_ACCOUNT_ID);
    assertTrue(operations.isEmpty());
  }

  @Test
  public void testSaveBatchOperation() {
    BatchOperation op = new BatchOperation(TEST_ACCOUNT_ID, null);
    String opId = op.getBatchId();

    // Confirm that loaded BatchOperation is the same as that saved.
    datastore.saveBatchOperation(op);
    BatchOperation op2 = datastore.loadBatchOperation(TEST_ACCOUNT_ID, opId);
    assertEquals(op, op2);

    // Confirm that one and only one BatchOperation has been saved.
    List<BatchOperation> operations = datastore.listBatchOperations(TEST_ACCOUNT_ID);
    assertEquals(1, operations.size());
  }

  @Test(expected = NotFoundException.class)
  public void testLoadBatchOperationThrows() {
    String nonExistentId = "abc123";
    BatchOperation op = datastore.loadBatchOperation(TEST_ACCOUNT_ID, nonExistentId);
  }

  @Test(expected = NotFoundException.class)
  public void testLoadBatchOperationsWrongAccount() {
    BatchOperation op = new BatchOperation(TEST_ACCOUNT_ID, null);
    String opId = op.getBatchId();

    // Should not load without the correct account Id.
    datastore.loadBatchOperation(TEST_ACCOUNT_ID2, opId);
  }

  @Test
  public void testDeleteBatchOperation() {
    List<String> urls = ImmutableList.of("http://www.example.com");

    BatchOperation op = new BatchOperation(TEST_ACCOUNT_ID, null);
    BatchSubOperation subOp = new BatchSubOperation(op, urls);
    op.addSubOperations(ImmutableList.of(subOp));

    String batchId = op.getBatchId();
    String subBatchId = subOp.getId();

    datastore.saveBatchOperation(op);
    datastore.saveBatchSubOperation(subOp);

    BatchOperation retOp = datastore.loadBatchOperation(TEST_ACCOUNT_ID, batchId);
    BatchSubOperation retSubOp =
        datastore.loadBatchSubOperation(TEST_ACCOUNT_ID, batchId, subBatchId);

    assertEquals(op, retOp);
    assertEquals(subOp, retSubOp);

    datastore.deleteBatchOperation(TEST_ACCOUNT_ID, batchId);

    try {
      BatchOperation delTestOp = datastore.loadBatchOperation(TEST_ACCOUNT_ID, batchId);
      fail("BatchOperation should have been removed from Datastore.");
    } catch (NotFoundException e) {
      assertTrue(e.getMessage().contains("No entity was found matching the key"));
    }
    try {
      BatchSubOperation delTestSubOp =
          datastore.loadBatchSubOperation(TEST_ACCOUNT_ID, batchId, subBatchId);
      fail("BatchSubOperation should have been removed from Datastore.");
    } catch (NotFoundException e) {
      assertTrue(e.getMessage().contains("No entity was found matching the key"));
    }
  }

  @Test
  public void testListHistoricBatchOperations()
      throws IllegalAccessException, NoSuchFieldException {
    // Set up 3 BatchOperation objects and set their creation dates o 3 specific times.
    Date earliest = new Date(1451606400);
    Date boundary = new Date(1456790400);
    Date latest = new Date(1462060800);

    Field created = BatchOperation.class.getDeclaredField("created");
    created.setAccessible(true);

    BatchOperation op1 = new BatchOperation(TEST_ACCOUNT_ID, null);
    BatchOperation op2 = new BatchOperation(TEST_ACCOUNT_ID, null);
    BatchOperation op3 = new BatchOperation(TEST_ACCOUNT_ID, null);

    created.set(op1, earliest);
    created.set(op2, boundary);
    created.set(op3, latest);

    datastore.saveBatchOperation(op1);
    datastore.saveBatchOperation(op2);
    datastore.saveBatchOperation(op3);

    // Expect that only the earliest BatchOperation is retrieved.
    List<BatchOperation> ops = datastore.listHistoricBatchOperations(boundary);

    assertEquals(1, ops.size());
    assertEquals(op1, ops.get(0));

    // Expect an empty list when using earliest as the boundary.
    List<BatchOperation> ops2 = datastore.listHistoricBatchOperations(earliest);
    assertTrue(ops2.isEmpty());
  }

  @Test
  public void testLoadBatchSubOperationExists() {
    List<String> urls = ImmutableList.of("http://www.example.com");

    BatchOperation op = new BatchOperation(TEST_ACCOUNT_ID, null);
    BatchSubOperation subOp = new BatchSubOperation(op, urls);
    op.addSubOperations(ImmutableList.of(subOp));
    String batchId = op.getBatchId();
    String subId = subOp.getId();

    datastore.saveBatchOperation(op);
    datastore.saveBatchSubOperation(subOp);

    BatchSubOperation subOp2 = datastore.loadBatchSubOperation(TEST_ACCOUNT_ID, batchId, subId);
    assertEquals(subOp, subOp2);
  }

  @Test
  public void testLoadBatchSubOperationThrows() {
    List<String> urls = ImmutableList.of("http://www.example.com");
    String nonExistentSubId = "abc123";

    BatchOperation op = new BatchOperation(TEST_ACCOUNT_ID, null);
    BatchSubOperation subOp = new BatchSubOperation(op, urls);
    op.addSubOperations(ImmutableList.of(subOp));

    datastore.saveBatchOperation(op);
    datastore.saveBatchSubOperation(subOp);

    try {
      BatchSubOperation subOp2 =
          datastore.loadBatchSubOperation(TEST_ACCOUNT_ID, op.getBatchId(), nonExistentSubId);
      fail("Loading BatchSubOperation with non-existent ID should throw exception.");
    } catch (NotFoundException e) {
      assertTrue(e.getMessage().contains("No entity was found matching the key"));
    }
  }

  @Test
  public void testDecrementSubOperationsRemaining() {
    List<String> urls = ImmutableList.of("http://www.example.com");
    List<String> urls2 = ImmutableList.of("http://www.example.org");

    BatchOperation op = new BatchOperation(TEST_ACCOUNT_ID, null);
    BatchSubOperation subOp = new BatchSubOperation(op, urls);
    BatchSubOperation subOp2 = new BatchSubOperation(op, urls);
    List<BatchSubOperation> subOps = ImmutableList.of(subOp, subOp2);
    op.addSubOperations(subOps);
    datastore.saveBatchOperationAndChildren(op, subOps);

    assertEquals(BatchOperationStatus.PROCESSING, op.getStatus());
    op.decrementRemainingSubOperations();
    assertEquals(BatchOperationStatus.PROCESSING, op.getStatus());
    op.decrementRemainingSubOperations();
    assertEquals(BatchOperationStatus.COMPLETE, op.getStatus());
  }

  @Test
  public void testSaveBatchOperationAndChildren() {
    List<String> urls = ImmutableList.of("http://www.example.com");
    List<String> urls2 = ImmutableList.of("http://www.example.org");

    BatchOperation op = new BatchOperation(TEST_ACCOUNT_ID, null);
    BatchSubOperation subOp = new BatchSubOperation(op, urls);
    BatchSubOperation subOp2 = new BatchSubOperation(op, urls);
    List<BatchSubOperation> subOps = ImmutableList.of(subOp, subOp2);
    op.addSubOperations(subOps);
    datastore.saveBatchOperationAndChildren(op, subOps);

    BatchOperation retOp = datastore.loadBatchOperation(TEST_ACCOUNT_ID, op.getBatchId());
    BatchSubOperation retSubOp =
        datastore.loadBatchSubOperation(TEST_ACCOUNT_ID, op.getBatchId(), subOp.getId());
    BatchSubOperation retSubOp2 =
        datastore.loadBatchSubOperation(TEST_ACCOUNT_ID, op.getBatchId(), subOp2.getId());

    assertEquals(op, retOp);
    assertEquals(subOp, retSubOp);
    assertEquals(subOp2, retSubOp2);
  }

  @Test
  public void testSaveBatchSubOperations() {
    List<String> urls = ImmutableList.of("http://www.example.com");
    List<String> urls2 = ImmutableList.of("http://www.example.org");

    BatchOperation op = new BatchOperation(TEST_ACCOUNT_ID, null);
    BatchSubOperation subOp = new BatchSubOperation(op, urls);
    BatchSubOperation subOp2 = new BatchSubOperation(op, urls);
    List<BatchSubOperation> subOps = ImmutableList.of(subOp, subOp2);
    op.addSubOperations(subOps);
    datastore.saveBatchOperation(op);
    datastore.saveBatchSubOperations(subOps);

    BatchOperation retOp = datastore.loadBatchOperation(TEST_ACCOUNT_ID, op.getBatchId());
    BatchSubOperation retSubOp =
        datastore.loadBatchSubOperation(TEST_ACCOUNT_ID, op.getBatchId(), subOp.getId());
    BatchSubOperation retSubOp2 =
        datastore.loadBatchSubOperation(TEST_ACCOUNT_ID, op.getBatchId(), subOp2.getId());

    assertEquals(op, retOp);
    assertEquals(subOp, retSubOp);
    assertEquals(subOp2, retSubOp2);
  }

  @Test
  public void testCreateKey() {
    SharedKey key = datastore.createKey();
    assertNotNull(key);
  }

  @Test
  public void testGetKey() {
    SharedKey key = datastore.createKey();
    SharedKey key2 = datastore.getKey();
    SharedKey key3 = datastore.getKey();

    // Confirm that repeated retrieval of keys gets the same value.
    assertEquals(key, key2);
    assertEquals(key, key3);
  }

  @Test
  public void testGetSettings() {
    Settings settings = datastore.createDefaultSettings();
    assertNotNull(settings);
  }
}
