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

import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.Assert.assertEquals;

import com.google.adwords.scripts.solutions.linkchecker.model.BatchOperation;
import com.google.adwords.scripts.solutions.linkchecker.model.BatchOperationStatus;
import com.google.adwords.scripts.solutions.linkchecker.model.BatchSubOperation;
import com.google.adwords.scripts.solutions.linkchecker.urlcheck.UrlCheckStatus;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Tests for the BatchOperationDetailResponse class. */
public class BatchOperationDetailResponseTest {
  private static final String TEST_ACCOUNT_ID = "123456";

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
  public void testShowErrorDetail()
      throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    List<String> urls = ImmutableList.of("http://www.example.com");

    BatchOperation op = new BatchOperation(TEST_ACCOUNT_ID, null);

    UrlCheckStatus status = UrlCheckStatus.fromUrl(urls.get(0));
    status.setStatus(UrlCheckStatus.Status.FAILURE, 404, "Not found");
    List<UrlCheckStatus> urlStatuses = Lists.newArrayList(status);

    BatchSubOperation subOp = new BatchSubOperation(op, urls);
    List<BatchSubOperation> subOps = ImmutableList.of(subOp);
    op.addSubOperations(subOps);

    Field statusesField = subOp.getClass().getDeclaredField("urlStatuses");
    statusesField.setAccessible(true);
    statusesField.set(subOp, urlStatuses);

    ofy().save().entity(op).now();
    ofy().save().entities(subOps).now();

    // When the overall BatchOperation is still processing, then errors should not be populated in
    // the response, even when there are known failures.
    BatchOperationDetailResponse response = BatchOperationDetailResponse.fromBatchOperation(op);
    assertEquals(BatchOperationStatus.PROCESSING, response.getStatus());
    assertEquals(0, response.getErrors().size());
    assertEquals(0, response.getCheckedUrlCount());

    Field statusField = op.getClass().getDeclaredField("status");
    statusField.setAccessible(true);
    statusField.set(op, BatchOperationStatus.COMPLETE);
    ofy().save().entity(op).now();

    // Only when the overall BatchOperation has entered the COMPLETE state should the response
    // contain a populated list of errors.
    BatchOperationDetailResponse response2 = BatchOperationDetailResponse.fromBatchOperation(op);
    assertEquals(BatchOperationStatus.COMPLETE, response2.getStatus());
    assertEquals(1, response2.getErrors().size());
    assertEquals(1, response2.getCheckedUrlCount());
  }
}
