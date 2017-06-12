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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.adwords.scripts.solutions.linkchecker.urlcheck.UrlCheckStatus;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.zip.GZIPOutputStream;
import org.junit.Before;
import org.junit.Test;

/** Tests for the UrlCheckerService class. */
public class UrlCheckerServiceTest {
  private static boolean factorySet = false;
  private static MockURLStreamHandler streamHandler;
  private UrlCheckerService urlCheckerService;

  @Before
  public void setUp() {
    if (!factorySet) {
      // URL Factory can only be set once per application.
      HttpURLConnection urlConnection = mock(HttpURLConnection.class);
      streamHandler = new MockURLStreamHandler(urlConnection);
      URL.setURLStreamHandlerFactory(streamHandler);
      factorySet = true;
    }
    urlCheckerService = new UrlCheckerService();
  }

  @Test
  public void check404ResponseTest() throws IOException {
    HttpURLConnection urlConnection = mock(HttpURLConnection.class);
    streamHandler.setConnection(urlConnection);
    UrlCheckStatus status = UrlCheckStatus.fromUrl("http://www.example.com");
    when(urlConnection.getResponseCode()).thenReturn(404);

    urlCheckerService.check(status, null);

    verify(urlConnection).setRequestMethod("HEAD");

    assertEquals(UrlCheckStatus.Status.FAILURE, status.getStatus());
    assertEquals(404, status.getHttpStatusCode());
    assertEquals("404", status.getMessage());
  }

  @Test
  public void check200ResponseTest() throws IOException {
    HttpURLConnection urlConnection = mock(HttpURLConnection.class);
    streamHandler.setConnection(urlConnection);
    UrlCheckStatus status = UrlCheckStatus.fromUrl("http://www.example.com");
    when(urlConnection.getResponseCode()).thenReturn(200);

    urlCheckerService.check(status, null);

    verify(urlConnection).setRequestMethod("HEAD");

    assertEquals(UrlCheckStatus.Status.SUCCESS, status.getStatus());
    assertEquals(200, status.getHttpStatusCode());
  }

  @Test
  public void check200FailureTextGzippedTest() throws IOException {
    HttpURLConnection urlConnection = mock(HttpURLConnection.class);
    streamHandler.setConnection(urlConnection);
    UrlCheckStatus status = UrlCheckStatus.fromUrl("http://www.example.com");
    when(urlConnection.getResponseCode()).thenReturn(200);

    String content = "This product is out of stock.";
    ByteArrayOutputStream obj = new ByteArrayOutputStream();
    GZIPOutputStream gzip = new GZIPOutputStream(obj);
    gzip.write(content.getBytes("UTF-8"));
    gzip.close();
    when(urlConnection.getInputStream()).thenReturn(new ByteArrayInputStream(obj.toByteArray()));
    when(urlConnection.getContentEncoding()).thenReturn("gzip");

    urlCheckerService.check(status, ImmutableList.of("out of stock"));

    verify(urlConnection, never()).setRequestMethod("HEAD");

    assertEquals(UrlCheckStatus.Status.FAILURE, status.getStatus());
    assertEquals(200, status.getHttpStatusCode());
    assertEquals("Content contains 'out of stock'", status.getMessage());
  }

  @Test
  public void check200FailureTextTest() throws IOException {
    HttpURLConnection urlConnection = mock(HttpURLConnection.class);
    streamHandler.setConnection(urlConnection);
    UrlCheckStatus status = UrlCheckStatus.fromUrl("http://www.example.com");
    when(urlConnection.getResponseCode()).thenReturn(200);

    String content = "This product is out of stock.";
    when(urlConnection.getInputStream()).thenReturn(new ByteArrayInputStream(content.getBytes()));
    when(urlConnection.getContentEncoding()).thenReturn("identity");

    urlCheckerService.check(status, ImmutableList.of("out of stock"));

    verify(urlConnection, never()).setRequestMethod("HEAD");

    assertEquals(UrlCheckStatus.Status.FAILURE, status.getStatus());
    assertEquals(200, status.getHttpStatusCode());
    assertEquals("Content contains 'out of stock'", status.getMessage());
  }

  @Test
  public void check200FailureTextPassTest() throws IOException {
    HttpURLConnection urlConnection = mock(HttpURLConnection.class);
    streamHandler.setConnection(urlConnection);
    UrlCheckStatus status = UrlCheckStatus.fromUrl("http://www.example.com");
    when(urlConnection.getResponseCode()).thenReturn(200);

    String content = "This product is in stock. 20 items remaining";
    when(urlConnection.getInputStream()).thenReturn(new ByteArrayInputStream(content.getBytes()));
    when(urlConnection.getContentEncoding()).thenReturn("identity");

    urlCheckerService.check(status, ImmutableList.of("out of stock"));

    verify(urlConnection, never()).setRequestMethod("HEAD");

    assertEquals(UrlCheckStatus.Status.SUCCESS, status.getStatus());
    assertEquals(200, status.getHttpStatusCode());
  }

  @Test
  public void checkNetworkError() throws IOException {
    HttpURLConnection urlConnection = mock(HttpURLConnection.class);
    streamHandler.setConnection(urlConnection);
    UrlCheckStatus status = UrlCheckStatus.fromUrl("http://www.example.com");
    when(urlConnection.getResponseCode()).thenThrow(new IOException("A network error occurred."));

    urlCheckerService.check(status, null);
    assertEquals(-1, status.getHttpStatusCode());
    assertEquals("A network error occurred.", status.getMessage());
  }

  @Test
  public void checkMalformedUrl() throws IOException {
    HttpURLConnection urlConnection = mock(HttpURLConnection.class);
    streamHandler.setConnection(urlConnection);
    UrlCheckStatus status = UrlCheckStatus.fromUrl("abcabcabcabc");

    urlCheckerService.check(status, null);
    assertEquals(-1, status.getHttpStatusCode());
    assertEquals("no protocol: abcabcabcabc", status.getMessage());
  }

  /** Mock stream handler for setting on the URL class. */
  public class MockURLStreamHandler extends URLStreamHandler implements URLStreamHandlerFactory {
    private HttpURLConnection urlConnection;

    public MockURLStreamHandler(HttpURLConnection urlConnection) {
      this.urlConnection = urlConnection;
    }

    public HttpURLConnection getConnection() {
      return urlConnection;
    }

    public void setConnection(HttpURLConnection connection) {
      this.urlConnection = connection;
    }

    protected HttpURLConnection openConnection() throws IOException {
      return urlConnection;
    }

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
      return this;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
      return urlConnection;
    }
  }
}
