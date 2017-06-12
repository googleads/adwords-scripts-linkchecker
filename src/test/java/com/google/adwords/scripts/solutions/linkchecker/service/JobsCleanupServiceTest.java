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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.adwords.scripts.solutions.linkchecker.datastore.Datastore;
import java.util.Calendar;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/** Tests for the JobsCleanupService. */
public class JobsCleanupServiceTest {
  private BatchOperationService batchOperationService;
  private Datastore datastore;
  private JobsCleanupService jobsCleanupService;

  @Before
  public void setUp() {
    batchOperationService = mock(BatchOperationService.class);
    datastore = mock(Datastore.class);
    jobsCleanupService = new JobsCleanupService(batchOperationService, datastore);
  }

  @Test
  public void cleanupTest() {
    long dateMillis =
        new Date().getTime() - (long) JobsCleanupService.OLD_JOB_CUTOFF_DAYS * 86400000L;
    Date expectedCutOffDate = new Date(dateMillis);
    Calendar expectedCutOff = Calendar.getInstance();
    expectedCutOff.setTime(expectedCutOffDate);

    ArgumentCaptor<Date> arg1 = ArgumentCaptor.forClass(Date.class);
    jobsCleanupService.cleanup();

    verify(batchOperationService).listHistoricBatchOperations(arg1.capture());
    Date actualCutOffDate = arg1.getValue();
    Calendar actualCutOff = Calendar.getInstance();
    actualCutOff.setTime(actualCutOffDate);
    assertEquals(expectedCutOff.get(Calendar.YEAR), actualCutOff.get(Calendar.YEAR));
    assertEquals(expectedCutOff.get(Calendar.DAY_OF_YEAR), actualCutOff.get(Calendar.DAY_OF_YEAR));
  }
}
