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

import com.google.adwords.scripts.solutions.linkchecker.urlcheck.UrlCheckStatus;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Provides the means to request web pages and check the returning HTTP status code, or determine
 * whether text indicative of a failure is in the page (e.g. "out of stock").
 */
public class UrlCheckerService {
  private static final int DEFAULT_TIMEOUT_MILLIS = 15000;
  private static final String DEFAULT_USER_AGENT = "GAE Link Checker";
  /**
   * Fetches a URL and updates the status to whether the fetch was a success or a failure.
   *
   * @param urlCheckStatus The details and status of the URL to be checked. Note that this object is
   *     modified in place with the results of the check.
   * @param failureMatchTexts Optional list of strings to search for on the retrieved page that
   *     indicate failure (e.g. "Out of stock").
   * @param userAgent The user-agent to set with each request.
   */
  public void check(
      UrlCheckStatus urlCheckStatus, List<String> failureMatchTexts, String userAgent) {
    HttpURLConnection urlConnection = null;
    
    System.setProperty("http.keepAlive", "false");
    try {
      URL url = new URL(urlCheckStatus.getUrl());
      urlConnection = (HttpURLConnection) url.openConnection();
      urlConnection.setConnectTimeout(DEFAULT_TIMEOUT_MILLIS);
      urlConnection.setReadTimeout(DEFAULT_TIMEOUT_MILLIS);
      // Aim for gzip if possible to reduce the use of the App Engine network quota.
      urlConnection.setRequestProperty("Accept-Encoding", "gzip");
      urlConnection.setRequestProperty("User-Agent", userAgent);
      
      // If there is no failure text to scan for, then no need to request the
      // entire body, a HEAD request will suffice and save on data transfer.
      if (failureMatchTexts == null || failureMatchTexts.isEmpty()) {
        urlConnection.setRequestMethod("HEAD");
      }
      
      // Ensure that 30x messages are honoured and cached versions are not used.
      urlConnection.setInstanceFollowRedirects(true);
      urlConnection.setUseCaches(false);
      
      int statusCode = urlConnection.getResponseCode();
      // A status code less than 400 indicates a non-error condition, however
      // if there is text to scan for such as "Out of stock", then this can
      // still cause the overall result to be a failure.
      if (statusCode < 400) {
        urlCheckStatus.setStatus(UrlCheckStatus.Status.SUCCESS, statusCode, null);
        if (failureMatchTexts != null && !failureMatchTexts.isEmpty()) {
          BufferedReader bufferedReader;
          if ("gzip".equals(urlConnection.getContentEncoding())) {
             bufferedReader = new BufferedReader(
               new InputStreamReader(new GZIPInputStream(urlConnection.getInputStream())));
          } else {
             bufferedReader = new BufferedReader(
               new InputStreamReader(urlConnection.getInputStream()));
          }
          
          String line;
          htmlcheck:
          while ((line = bufferedReader.readLine()) != null) {
            for (String failureMatchText : failureMatchTexts) {
              if (line.contains(failureMatchText)) {
                urlCheckStatus.setStatus(
                    UrlCheckStatus.Status.FAILURE,
                    statusCode,
                    "Content contains '" + failureMatchText + "'");
                break htmlcheck;
              }
            }
          }
          bufferedReader.close();
        }
      } else {
        // Error due to status code being >= 400
        urlCheckStatus.setStatus(
            UrlCheckStatus.Status.FAILURE, statusCode, String.valueOf(statusCode));
      }
    } catch (IOException e) {
      // Error due to some other condition such as network error.
      urlCheckStatus.setStatus(UrlCheckStatus.Status.FAILURE, -1, e.getMessage());
    } finally {
      if (urlConnection != null) {
        urlConnection.disconnect();
      }
    }
  }

  public void check(UrlCheckStatus urlCheckStatus, List<String> failureMatchTexts) {
    check(urlCheckStatus, failureMatchTexts, DEFAULT_USER_AGENT);
  }
}
