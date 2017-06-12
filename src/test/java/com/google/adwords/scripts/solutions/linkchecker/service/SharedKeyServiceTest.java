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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.adwords.scripts.solutions.linkchecker.datastore.Datastore;
import org.junit.Before;
import org.junit.Test;

/** Tests for the SharedKeyService class. */
public class SharedKeyServiceTest {
  private Datastore datastore;
  private SharedKeyService sharedKeyService;

  @Before
  public void setUp() {
    datastore = mock(Datastore.class);
    sharedKeyService = new SharedKeyService(datastore);
  }

  @Test
  public void getKeyTest() {
    sharedKeyService.getKey();
    verify(datastore, times(1)).createKey();
    verify(datastore, times(1)).getKey();
  }
}
