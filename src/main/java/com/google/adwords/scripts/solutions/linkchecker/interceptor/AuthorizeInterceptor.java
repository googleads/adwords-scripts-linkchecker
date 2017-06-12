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

package com.google.adwords.scripts.solutions.linkchecker.interceptor;

import com.google.adwords.scripts.solutions.linkchecker.service.SharedKeyService;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.utils.SystemProperty;
import com.google.inject.Inject;
import com.google.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * A class to apply to BatchLinkChecker API methods to ensure that the requesting client is
 * supplying the valid shared key. The shared key is not required when deployed as a development
 * instance.
 */
public class AuthorizeInterceptor implements MethodInterceptor {
  private final Provider<HttpServletRequest> httpServletRequestProvider;
  private final Provider<SharedKeyService> sharedKeyService;

  @Inject
  public AuthorizeInterceptor(
      Provider<SharedKeyService> sharedKeyService,
      Provider<HttpServletRequest> httpServletRequestProvider) {
    this.httpServletRequestProvider = httpServletRequestProvider;
    this.sharedKeyService = sharedKeyService;
  }

  @Override
  public Object invoke(MethodInvocation mi) throws Throwable {
    String serverKey = sharedKeyService.get().getKey().getKeyText();
    HttpServletRequest hsr = httpServletRequestProvider.get();

    String authHeader = hsr.getHeader("Authorization");
    if (authHeader != null && serverKey != null && authHeader.equals(serverKey)) {
      return mi.proceed();
    } else if (SystemProperty.environment.value() == SystemProperty.Environment.Value.Development) {
      return mi.proceed();
    }
    throw new UnauthorizedException("Shared key incorrect");
  }
}
