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

package com.google.adwords.scripts.solutions.linkchecker.tasks;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.adwords.scripts.solutions.linkchecker.datastore.Datastore;
import com.google.adwords.scripts.solutions.linkchecker.model.BatchOperation;
import com.google.adwords.scripts.solutions.linkchecker.model.BatchSubOperation;
import com.google.adwords.scripts.solutions.linkchecker.model.Settings;
import com.google.adwords.scripts.solutions.linkchecker.service.SettingsService;
import com.google.adwords.scripts.solutions.linkchecker.service.UrlCheckerService;
import com.google.adwords.scripts.solutions.linkchecker.urlcheck.UrlCheckStatus;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Tests for the UrlCheckTask class. */
public class UrlCheckTaskTest {
  private static final String TEST_ACCOUNT_ID = "123456";

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalTaskQueueTestConfig());

  private Datastore datastore;
  private SettingsService settingsService;
  private Settings settings;
  private UrlCheckerService urlCheckerService;

  @Before
  public void setUp() {
    helper.setUp();
    datastore = mock(Datastore.class);
    settingsService = mock(SettingsService.class);
    settings = Settings.createDefaultSettings();
    urlCheckerService = mock(UrlCheckerService.class);
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void testTaskGetsRun()
      throws InterruptedException, NoSuchMethodException, IllegalAccessException,
          IllegalArgumentException, InvocationTargetException {
    final String accountId = "123";
    final String opId = "456";
    final String subOpId = "789";
    final List<String> failureMatchTexts = ImmutableList.of("out of stock");

    UrlCheckStatus status = UrlCheckStatus.fromUrl("http://www.example.com");
    BatchOperation op = mock(BatchOperation.class);
    BatchSubOperation subOp = mock(BatchSubOperation.class);
    when(subOp.getParent()).thenReturn(op);
    when(op.getFailureMatchTexts()).thenReturn(failureMatchTexts);

    when(settingsService.getSettings()).thenReturn(settings);
    when(datastore.loadBatchSubOperation(accountId, opId, subOpId)).thenReturn(subOp);

    when(subOp.getUrlStatuses()).thenReturn(ImmutableList.of(status));

    UrlCheckTask task = new UrlCheckTask(accountId, opId, subOpId);
    task.check(datastore, settingsService, urlCheckerService);

    verify(urlCheckerService).check(status, failureMatchTexts, settings.getUserAgentString());
    verify(datastore).saveBatchSubOperation(subOp);
    verify(datastore).decrementSubOperationsRemaining(accountId, opId);
  }

  @Test
  public void testTaskSavedForRetry()
      throws InterruptedException, NoSuchMethodException, IllegalAccessException,
          IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
    final String accountId = "123";
    final String opId = "456";
    final String subOpId = "789";
    final List<String> failureMatchTexts = ImmutableList.of("out of stock");

    // Add considerably more URLs than can be tested in 10s, at 1/s.
    List<UrlCheckStatus> statuses = Lists.newArrayList();
    for (int i = 0; i < 100; i++) {
      statuses.add(UrlCheckStatus.fromUrl("http://www.example.com/" + i));
    }
    BatchOperation op = mock(BatchOperation.class);
    BatchSubOperation subOp = mock(BatchSubOperation.class);
    when(subOp.getParent()).thenReturn(op);
    when(op.getFailureMatchTexts()).thenReturn(failureMatchTexts);

    when(settingsService.getSettings()).thenReturn(settings);
    when(datastore.loadBatchSubOperation(accountId, opId, subOpId)).thenReturn(subOp);

    when(subOp.getUrlStatuses()).thenReturn(statuses);

    // Set the maximum execution to only 10 seconds, for the purpose of testing.
    // The default URL check rate is 1/s.
    UrlCheckTask task = new UrlCheckTask(accountId, opId, subOpId);
    task.setMaxLoopTimeNanoSeconds(10_000_000_000L);
    task.check(datastore, settingsService, urlCheckerService);

    // Should manage 10 checks when rate limited in 10 seconds, but could be 9 or 11
    // if the clock timing isn't perfect.
    verify(urlCheckerService, atMost(11))
        .check((UrlCheckStatus) any(), (List<String>) any(), eq(settings.getUserAgentString()));
    verify(urlCheckerService, atLeast(9))
        .check((UrlCheckStatus) any(), (List<String>) any(), eq(settings.getUserAgentString()));

    // Indicates that the batch has been saved and marked for resumption.
    verify(datastore).saveBatchSubOperation(subOp);
    // As operation is to be resumed, the decrement of remaining sub ops should not take place.
    verify(datastore, never()).decrementSubOperationsRemaining(accountId, opId);
  }
}
