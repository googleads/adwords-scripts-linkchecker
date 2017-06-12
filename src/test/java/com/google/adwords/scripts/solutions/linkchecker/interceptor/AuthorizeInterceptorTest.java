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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.adwords.scripts.solutions.linkchecker.LinkCheckerAuthorizationTestModule;
import com.google.adwords.scripts.solutions.linkchecker.model.SharedKey;
import com.google.adwords.scripts.solutions.linkchecker.service.SharedKeyService;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import javax.servlet.http.HttpServletRequest;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Tests for the AuthorizeInterceptor class. */
public class AuthorizeInterceptorTest {
  private Injector injector;
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  public AuthorizeInterceptorTest() {}

  @Before
  public void setUp() {
    helper.setUp();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test(expected = UnauthorizedException.class)
  public void testUnauthorizedRequest() throws Throwable {
    SharedKey sk = SharedKey.createRandom();
    SharedKeyService sks = mock(SharedKeyService.class);
    when(sks.getKey()).thenReturn(sk);

    HttpServletRequest hsr = mock(HttpServletRequest.class);
    when(hsr.getHeader("Authorization")).thenReturn("abc123");

    injector = Guice.createInjector(new LinkCheckerAuthorizationTestModule(hsr, sks));
    AuthorizeInterceptor ai = injector.getInstance(AuthorizeInterceptor.class);
    MethodInvocation mi = mock(MethodInvocation.class);
    when(mi.proceed()).thenReturn(null);

    ai.invoke(mi);
  }

  @Test
  public void testAuthorizedRequest() throws Throwable {
    SharedKey sk = SharedKey.createRandom();
    SharedKeyService sks = mock(SharedKeyService.class);
    when(sks.getKey()).thenReturn(sk);

    HttpServletRequest hsr = mock(HttpServletRequest.class);
    when(hsr.getHeader("Authorization")).thenReturn(sk.getKeyText());

    injector = Guice.createInjector(new LinkCheckerAuthorizationTestModule(hsr, sks));
    AuthorizeInterceptor ai = injector.getInstance(AuthorizeInterceptor.class);
    MethodInvocation mi = mock(MethodInvocation.class);

    ai.invoke(mi);
    verify(mi).proceed();
  }
}
