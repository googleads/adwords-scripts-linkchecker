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

import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Tests for BatchOperation class. */
public class BatchOperationTest {
  private static final String TEST_ACCOUNT_ID = "123456";
  private static final int UUID_LENGTH = 36;

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private Closeable closeable;

  @Before
  public void setUp() {
    helper.setUp();
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
  public void testCreate() {
    BatchOperation op = new BatchOperation(TEST_ACCOUNT_ID, null);

    assertEquals(TEST_ACCOUNT_ID, op.getAccountId());
    assertNull(op.getFailureMatchTexts());
    assertEquals(UUID_LENGTH, op.getBatchId().length());
    assertEquals(BatchOperationStatus.COMPLETE, op.getStatus());
  }

  @Test
  public void testAddSubOperations() {
    List<String> urls = ImmutableList.of("http://www.example.com");

    BatchOperation op = new BatchOperation(TEST_ACCOUNT_ID, null);
    assertEquals(BatchOperationStatus.COMPLETE, op.getStatus());

    BatchSubOperation subOp = new BatchSubOperation(op, urls);
    List<BatchSubOperation> subOps = ImmutableList.of(subOp, subOp);
    op.addSubOperations(subOps);
    ofy().save().entity(op).now();
    ofy().save().entities(subOps).now();

    assertEquals(BatchOperationStatus.PROCESSING, op.getStatus());

    List<BatchSubOperation> retSubOps = op.getSubOperations();
    assertArrayEquals(subOps.toArray(), retSubOps.toArray());
  }

  @Test
  public void testFailureMatchTexts() {
    List<String> failureMatchTexts = ImmutableList.of("Out of stock");

    BatchOperation op = new BatchOperation(TEST_ACCOUNT_ID, failureMatchTexts);
    assertEquals(BatchOperationStatus.COMPLETE, op.getStatus());
    assertEquals(failureMatchTexts, op.getFailureMatchTexts());
  }

  @Test
  public void testDecrementRemainingSubOperations() {
    List<String> urls = ImmutableList.of("http://www.example.com");

    BatchOperation op = new BatchOperation(TEST_ACCOUNT_ID, null);
    BatchSubOperation subOp = new BatchSubOperation(op, urls);
    List<BatchSubOperation> subOps = ImmutableList.of(subOp);
    op.addSubOperations(subOps);
    ofy().save().entity(op).now();
    ofy().save().entities(subOps).now();

    assertEquals(BatchOperationStatus.PROCESSING, op.getStatus());
    op.decrementRemainingSubOperations();
    assertEquals(BatchOperationStatus.COMPLETE, op.getStatus());
  }
}
