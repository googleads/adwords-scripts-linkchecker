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

package com.google.adwords.scripts.solutions.linkchecker;

import com.google.adwords.scripts.solutions.linkchecker.service.JobsCleanupService;
import com.google.api.server.spi.guice.EndpointsModule;
import com.google.inject.Scopes;
 
/**
 * Module to configure the CronServlet, which provides a single URL for requesting the deletion of
 * old jobs.
 */
public class CronModule extends EndpointsModule {
  @Override
  protected void configureServlets() {
    bind(CronServlet.class).in(Scopes.SINGLETON);
    serve(JobsCleanupService.INDEX_BUILD_PATH).with(CronServlet.class);
  }
}
