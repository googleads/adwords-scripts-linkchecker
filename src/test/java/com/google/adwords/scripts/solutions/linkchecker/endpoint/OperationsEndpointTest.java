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

package com.google.adwords.scripts.solutions.linkchecker.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.adwords.scripts.solutions.linkchecker.model.BatchOperation;
import com.google.adwords.scripts.solutions.linkchecker.request.UrlCheckRequest;
import com.google.adwords.scripts.solutions.linkchecker.response.BatchOperationDetailResponse;
import com.google.adwords.scripts.solutions.linkchecker.service.BatchOperationService;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.ServiceUnavailableException;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Tests for the OperationsEndpoint class. */
public class OperationsEndpointTest {
  private final LocalServiceTestHelper testHelper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private static final String TEST_ACCOUNT_ID = "123456";

  @Before
  public void setUp() throws Exception {
    testHelper.setUp();
  }

  @After
  public void tearDown() throws Exception {
    testHelper.tearDown();
  }

  @Test
  public void testGetNotFound() {
    com.googlecode.objectify.NotFoundException exception =
        mock(com.googlecode.objectify.NotFoundException.class);
    String batchId = "123456abcdef";

    BatchOperationService batchOperationService = mock(BatchOperationService.class);
    when(batchOperationService.getOperationById(anyString(), anyString())).thenThrow(exception);

    OperationsEndpoint endpoint = new OperationsEndpoint(batchOperationService);
    try {
      endpoint.get(TEST_ACCOUNT_ID, batchId);
      fail("Call to OperationsEndpoint should result in a NotFoundException.");
    } catch (com.google.api.server.spi.response.NotFoundException e) {
      assertTrue(e.getMessage().contains(batchId));
    }
  }

  @Test
  public void testGet() throws NotFoundException {
    BatchOperationService batchOperationService = mock(BatchOperationService.class);
    BatchOperation op = new BatchOperation(TEST_ACCOUNT_ID, null);

    String accountId = op.getAccountId();
    String batchId = op.getBatchId();

    when(batchOperationService.getOperationById(accountId, batchId))
        .thenReturn(BatchOperationDetailResponse.fromBatchOperation(op));

    OperationsEndpoint endpoint = new OperationsEndpoint(batchOperationService);
    BatchOperationDetailResponse response = endpoint.get(accountId, batchId);

    assertEquals(batchId, response.getBatchId());
  }

  @Test
  public void testDeleteNotFound() {
    com.googlecode.objectify.NotFoundException exception =
        mock(com.googlecode.objectify.NotFoundException.class);
    String batchId = "123456abcdef";

    BatchOperationService batchOperationService = mock(BatchOperationService.class);
    doThrow(exception).when(batchOperationService).deleteBatchOperation(anyString(), anyString());

    OperationsEndpoint endpoint = new OperationsEndpoint(batchOperationService);
    try {
      endpoint.delete(TEST_ACCOUNT_ID, batchId);
      fail("Call to OperationsEndpoint should result in a NotFoundException.");
    } catch (com.google.api.server.spi.response.NotFoundException e) {
      assertTrue(e.getMessage().contains(batchId));
    }
  }

  @Test
  public void testPostEmptyRequest() throws InterruptedException, ServiceUnavailableException {
    UrlCheckRequest request = new UrlCheckRequest(null);
    UrlCheckRequest request2 = new UrlCheckRequest(new ArrayList<String>());

    BatchOperationService batchOperationService = mock(BatchOperationService.class);
    OperationsEndpoint endpoint = new OperationsEndpoint(batchOperationService);
    try {
      endpoint.add(TEST_ACCOUNT_ID, request);
      fail("Call to OperationsEndpoint should result in a NullPointerException.");
    } catch (NullPointerException e) {
      assertTrue(e.getMessage().contains("The add method must include a list of URLs"));
    }

    try {
      endpoint.add(TEST_ACCOUNT_ID, request2);
      fail("Call to OperationsEndpoint should result in a NullPointerException.");
    } catch (NullPointerException e) {
      assertTrue(e.getMessage().contains("The add method must include a list of URLs"));
    }
  }

  @Test
  public void testPost() throws InterruptedException, ServiceUnavailableException {
    UrlCheckRequest request = new UrlCheckRequest(ImmutableList.of("http://www.bbc.co.uk"));

    BatchOperationService batchOperationService = mock(BatchOperationService.class);
    when(batchOperationService.createNewBatchOperation(TEST_ACCOUNT_ID, request))
        .thenReturn("123456");
    OperationsEndpoint endpoint = new OperationsEndpoint(batchOperationService);

    List<String> add = endpoint.add(TEST_ACCOUNT_ID, request);

    assertEquals(1, add.size());
    assertEquals("123456", add.get(0));
  }
}
