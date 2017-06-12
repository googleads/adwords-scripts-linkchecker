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

package com.google.adwords.scripts.solutions.linkchecker.request;

import java.util.List;
 
/**
 * Class to represent a URL to check.
 */
public class UrlCheckRequest {
  List<String> urls;
  // Optional Strings which can be checked for in the body of the web page
  // and if found will indicate a failure. For example, could be "Out of Stock".
  List<String> failureMatchTexts;
  
  public UrlCheckRequest() {};
  
  public UrlCheckRequest(List<String> urls) {
    this(urls, null);
  }
  
  public UrlCheckRequest(List<String> urls, List<String> failureMatchTexts) {
    this.urls = urls;
    this.failureMatchTexts = failureMatchTexts;
  }
  
  public List<String> getUrls() {
    return urls;
  }
  
  public List<String> getFailureMatchTexts() {
    return failureMatchTexts;
  }
  
  public boolean hasFailureMatchText() {
    return failureMatchTexts != null && failureMatchTexts.size() > 0;
  }
}
