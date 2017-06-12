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

package com.google.adwords.scripts.solutions.linkchecker.model;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.Assert.assertEquals;

import com.google.appengine.repackaged.com.google.common.collect.Lists;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Tests for the BatchSubOperation class. */
public class BatchSubOperationTest {
  private static final String TEST_ACCOUNT_ID = "123456";
  private static final int UUID_LENGTH = 36;

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private Closeable closeable;

  @Before
  public void setUp() {
    helper.setUp();
    ObjectifyService.register(BatchOperation.class);
    ObjectifyService.register(BatchSubOperation.class);
    closeable = ObjectifyService.begin();
  }

  @After
  public void tearDown() {
    closeable.close();
    helper.tearDown();
  }

  @Test
  public void testCreate() {
    List<String> urls = ImmutableList.of("http://www.example.com");
    BatchOperation op = new BatchOperation(TEST_ACCOUNT_ID, null);
    BatchSubOperation subOp = new BatchSubOperation(op, urls);
    ofy().save().entity(op).now();
    ofy().save().entities(subOp).now();

    assertEquals(UUID_LENGTH, subOp.getId().length());
    assertEquals(op, subOp.getParent());
    assertEquals(1, subOp.getUrlStatuses().size());
    assertEquals(urls.get(0), subOp.getUrlStatuses().get(0).getUrl());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTooManyUrls() {
    List<String> urls = Lists.newArrayList();
    // Add one too many URLs.
    for (int i = 0; i < BatchSubOperation.MAX_URLS + 1; i++) {
      urls.add("http://test" + i);
    }
    BatchOperation op = new BatchOperation(TEST_ACCOUNT_ID, null);
    BatchSubOperation subOp = new BatchSubOperation(op, urls);
  }
}
