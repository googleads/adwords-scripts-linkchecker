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

import com.google.adwords.scripts.solutions.linkchecker.annotation.Authorize;
import com.google.adwords.scripts.solutions.linkchecker.annotation.Authorize.Type;
import com.google.adwords.scripts.solutions.linkchecker.model.BatchOperation;
import com.google.adwords.scripts.solutions.linkchecker.request.UrlCheckRequest;
import com.google.adwords.scripts.solutions.linkchecker.response.BatchOperationDetailResponse;
import com.google.adwords.scripts.solutions.linkchecker.service.BatchOperationService;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.ServiceUnavailableException;
import com.google.apphosting.api.ApiProxy.OverQuotaException;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Named;

/** Defines v1 of a BatchLinkChecker API */
@Api(name = "batchLinkChecker", version = "v1", title = "Batch Link Checker API")
public class OperationsEndpoint {
  private final BatchOperationService batchOperationService;

  @Inject
  public OperationsEndpoint(BatchOperationService batchOperationService) {
    this.batchOperationService = batchOperationService;
  }

  /**
   * Retrieves the specific details of a {@code BatchOperation}. This includes a list of any failed
   * URLs and the total number of URLs checked, where a {@code BatchOperation} is complete. These
   * details are omitted where a {@code BatchOperation} is still in the state of being processed.
   *
   * @param accountId The account ID. Used to distinguish jobs submitted by different accounts.
   * @param batchId The ID returned from either the {@code add} or {@code list} methods.
   * @return A {@code BatchOperationDetailResponse}
   * @throws NotFoundException An error is thrown when no {@code BatchOperation} of the given ID is
   *     found.
   */
  @Authorize(value = Type.SHARED_KEY)
  @ApiMethod(path = "account/{accountId}/batchoperation/{id}")
  public BatchOperationDetailResponse get(
      @Named("accountId") String accountId, @Named("id") String batchId) throws NotFoundException {
    try {
      return batchOperationService.getOperationById(accountId, batchId);
    } catch (com.googlecode.objectify.NotFoundException e) {
      throw new NotFoundException("BatchOperation not found for id " + batchId);
    }
  }

  /**
   * Deletes the specified {@code BatchOperation} from the Datastore.
   *
   * @param accountId
   * @param batchId
   * @throws NotFoundException
   */
  @Authorize(value = Type.SHARED_KEY)
  @ApiMethod(path = "account/{accountId}/batchoperation/{id}", httpMethod = HttpMethod.DELETE)
  public void delete(@Named("accountId") String accountId, @Named("id") String batchId)
      throws NotFoundException {
    try {
      batchOperationService.deleteBatchOperation(accountId, batchId);
    } catch (com.googlecode.objectify.NotFoundException e) {
      throw new NotFoundException("BatchOperation not found with id: " + batchId);
    }
  }

  /**
   * Lists existing {@code BatchOperation} objects for a given account.
   *
   * @param accountId The ID of the account.
   * @return A list of BatchOperation objects.
   */
  @Authorize(value = Type.SHARED_KEY)
  @ApiMethod(path = "account/{accountId}/batchoperation")
  public List<BatchOperation> list(@Named("accountId") String accountId) {
    return batchOperationService.listBatchOperations(accountId);
  }

  /**
   * Creates a new {@code BatchOperation} to check the status of a list of URLs.
   *
   * @param accountId The ID of the account to associate this request with.
   * @param urls An {@code UrlList} object containing definitions of the URLs to check.
   * @return A list with a single entry: The ID of the newly created {@code BatchOperation} job.
   * @throws InterruptedException, ServiceUnavailableException
   */
  @Authorize(value = Type.SHARED_KEY)
  @ApiMethod(httpMethod = HttpMethod.POST, path = "account/{accountId}/batchoperation")
  public List<String> add(@Named("accountId") String accountId, UrlCheckRequest request)
      throws InterruptedException, ServiceUnavailableException {
    if (request.getUrls() == null || request.getUrls().isEmpty()) {
      throw new NullPointerException("The add method must include a list of URLs.");
    }

    List<String> response = new ArrayList<>();
    try {
      String id = batchOperationService.createNewBatchOperation(accountId, request);
      response.add(id);
    } catch (OverQuotaException e) {
      throw new ServiceUnavailableException("Cannot add URLs: No available quota.");
    }
    return response;
  }
}
