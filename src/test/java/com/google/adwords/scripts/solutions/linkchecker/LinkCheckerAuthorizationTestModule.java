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
import com.google.adwords.scripts.solutions.linkchecker.interceptor.AuthorizeInterceptor;
import com.google.adwords.scripts.solutions.linkchecker.service.SharedKeyService;
import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import javax.servlet.http.HttpServletRequest;

/** Guice module for use in testing the AuthorizeInterceptor class. */
public class LinkCheckerAuthorizationTestModule extends AbstractModule {
  private final HttpServletRequest httpServletRequest;
  private final SharedKeyService sharedKeyService;

  public LinkCheckerAuthorizationTestModule(
      HttpServletRequest httpServletRequest, SharedKeyService sharedKeyService) {
    this.httpServletRequest = httpServletRequest;
    this.sharedKeyService = sharedKeyService;
  }

  @Override
  protected void configure() {
    // Interceptor is used to inspect requests to the Servlet, and where annotated with Authorize
    // ensure that the request contains the appropriate Authorization header.
    bind(HttpServletRequest.class).toInstance(httpServletRequest);
    bind(SharedKeyService.class).toInstance(sharedKeyService);
    bindInterceptor(
        Matchers.any(),
        Matchers.annotatedWith(Authorize.class),
        new AuthorizeInterceptor(
            getProvider(SharedKeyService.class), getProvider(HttpServletRequest.class)));
  }
}
