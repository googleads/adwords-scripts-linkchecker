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

import com.google.adwords.scripts.solutions.linkchecker.datastore.Datastore;
import com.google.adwords.scripts.solutions.linkchecker.model.BatchSubOperation;
import com.google.adwords.scripts.solutions.linkchecker.model.Settings;
import com.google.adwords.scripts.solutions.linkchecker.service.SettingsService;
import com.google.adwords.scripts.solutions.linkchecker.service.UrlCheckerService;
import com.google.adwords.scripts.solutions.linkchecker.urlcheck.UrlCheckStatus;
import com.google.appengine.api.taskqueue.DeferredTask;
import com.google.appengine.api.taskqueue.DeferredTaskContext;
import com.google.apphosting.api.ApiProxy.OverQuotaException;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.List;

/**
 * Provides a Task that can be run by the TaskQueue to check URLs for as much of the alloted 10mins
 * as possible. Note some points about this implementation:
 * 1.  The {@code UrlCheckerService} does not use connection timeouts to try to limit the length of
 *     each connection. This is because they are actually difficult to control: It is hard to
 *     specify that a connection must have *finished* within X seconds, the available methods relate
 *     more to the time in which some connection or some data must have been read (but not all).
 * 2.  Where the allotted URLs for this task are not completed in the time, the results so far are
 *     saved to Datastore and the Task is then marked for retry. Those URLs that have not been
 *     checked in each iteration will then be checked on the next attempt.
 */
public class UrlCheckTask implements DeferredTask {
  private final String accountId;
  private final String parentId;
  private final String taskId;
  private long maxLoopTimeNanos = MAX_LOOP_TIME_NANO_SECONDS;

  private static @Inject Injector injector;

  // Set the maximum time to loop to about 9 mins 30. This allows time to save
  // before exiting.
  private static final long MAX_LOOP_TIME_NANO_SECONDS = (long) (9.5 * 60_000_000_000L);
  private static final long ONE_MINUTE_NANO_SECONDS = 60_000_000_000L;
  private static final long MIN_SLEEP_TIME_NANO_SECONDS = 50_000_000L;

  public UrlCheckTask(String accountId, String parentId, String taskId) {
    this.accountId = accountId;
    this.parentId = parentId;
    this.taskId = taskId;
  }

  @Override
  public void run() {
    Datastore datastore = injector.getInstance(Datastore.class);
    SettingsService settingsService = injector.getInstance(SettingsService.class);
    UrlCheckerService urlCheckerService = injector.getInstance(UrlCheckerService.class);
    check(datastore, settingsService, urlCheckerService);
  }
  
  @VisibleForTesting
  void check(Datastore datastore, SettingsService settingsService,
      UrlCheckerService urlCheckerService) {
    Settings settings = settingsService.getSettings();
    long nanosPerUrl = ONE_MINUTE_NANO_SECONDS / settings.getRateInChecksPerMinute();

    BatchSubOperation subOp = datastore.loadBatchSubOperation(accountId, parentId, taskId);
    List<String> failureMatchTexts = subOp.getParent().getFailureMatchTexts();

    long startTime = System.nanoTime();
    List<UrlCheckStatus> statuses = subOp.getUrlStatuses();
    int numUrlsChecked = 0;
    for (UrlCheckStatus status : statuses) {

      long currTime = System.nanoTime();
      if (currTime - startTime > maxLoopTimeNanos) {
        // Not all of the available URLs have been checked in the time. Save what work has been done
        // and mark this task for retry (to continue with the unprocessed URLs).
        datastore.saveBatchSubOperation(subOp);
        DeferredTaskContext.markForRetry();
        return;
      }
      if (status.getStatus() == UrlCheckStatus.Status.NOT_STARTED) {
        try {
          urlCheckerService.check(status, failureMatchTexts, settings.getUserAgentString());
        } catch (OverQuotaException e) {
          // If there has been too much use of the network, save progress to this point and mark the
          // task for retry - the remaining URLs will be picked up then. It is only necessary to
          // save progress if some has been made, otherwise avoid the hit on the Datastore quota.
          if (numUrlsChecked > 0) {
            datastore.saveBatchSubOperation(subOp);
          }
          DeferredTaskContext.markForRetry();
          return;
        }
        numUrlsChecked++;

        // To control the rate of processing, compare the time taken for all URLs in this task so far
        // with the time expected by the rate in the settings. If the expected time is sufficiently
        // greater than the actual time, sleep until the two are the same.
        long timeInHand = numUrlsChecked * nanosPerUrl - System.nanoTime() + startTime;
        if (timeInHand > MIN_SLEEP_TIME_NANO_SECONDS) {
          try {
            Thread.sleep(timeInHand / 1000000);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }
    }
    
    // All URLs in this task have been checked. The results are saved, and then the number of
    // remaining BatchSubOperations for the parent is decremented, with the aim that when this
    // reaches zero, the overall parent is also marked as complete.
    datastore.saveBatchSubOperation(subOp);
    datastore.decrementSubOperationsRemaining(accountId, parentId);
  }
  
  public void setMaxLoopTimeNanoSeconds(long maxLoopTimeNanos) {
    this.maxLoopTimeNanos = maxLoopTimeNanos;
  }
}
