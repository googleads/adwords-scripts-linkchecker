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

import com.google.adwords.scripts.solutions.linkchecker.annotation.Authorize;
import com.google.adwords.scripts.solutions.linkchecker.endpoint.OperationsEndpoint;
import com.google.adwords.scripts.solutions.linkchecker.endpoint.SettingsEndpoint;
import com.google.adwords.scripts.solutions.linkchecker.interceptor.AuthorizeInterceptor;
import com.google.adwords.scripts.solutions.linkchecker.service.SharedKeyService;
import com.google.adwords.scripts.solutions.linkchecker.tasks.UrlCheckTask;
import com.google.api.server.spi.guice.GuiceSystemServiceServletModule;
import com.google.inject.matcher.Matchers;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

class LinkCheckerServletModule extends GuiceSystemServiceServletModule {

  @Override
  protected void configureServlets() {
    super.configureServlets();

    Set<Class<?>> serviceClasses = new HashSet<>();
    serviceClasses.add(OperationsEndpoint.class);
    serviceClasses.add(SettingsEndpoint.class);
    requestStaticInjection(UrlCheckTask.class);

    // Interceptor is used to inspect requests to the Servlet, and where annotated with Authorize
    // ensure that the request contains the appropriate Authorization header.
    bindInterceptor(
        Matchers.any(),
        Matchers.annotatedWith(Authorize.class),
        new AuthorizeInterceptor(
            getProvider(SharedKeyService.class), getProvider(HttpServletRequest.class)));

    this.serveGuiceSystemServiceServlet("/_ah/spi/*", serviceClasses);
  }
}
