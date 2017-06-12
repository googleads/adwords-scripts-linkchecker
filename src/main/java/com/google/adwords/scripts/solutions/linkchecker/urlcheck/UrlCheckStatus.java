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

package com.google.adwords.scripts.solutions.linkchecker.urlcheck;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import java.util.UUID;

/**
 * Represents the status of a given URL check, including whether it has yet been checked and the
 * outcome.
 */
@Cache
@Entity
public class UrlCheckStatus {
  /**
   * Represents the status of the job. Initial state is always NOT_STARTED, with the other states
   * representing final states.
   */
  public enum Status {
    SUCCESS,
    FAILURE,
    NOT_STARTED
  }

  @Id private final String id = UUID.randomUUID().toString();
  private String url;

  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
  private Status status;

  private String message;
  private int httpStatusCode;

  public UrlCheckStatus() {};

  private UrlCheckStatus(String url, String message) {
    this.url = url;
    this.status = Status.NOT_STARTED;
    this.message = message;
  }

  /**
   * Creates a new instance to hold the status based on URL.
   *
   * @param url The URL to check.
   * @return a new {@code UrlCheckStatus} object.
   */
  public static UrlCheckStatus fromUrl(String url) {
    return new UrlCheckStatus(url, "unchecked-error");
  }

  public Status getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public String getUrl() {
    return url;
  }

  public int getHttpStatusCode() {
    return httpStatusCode;
  }

  public void setStatus(Status status, int httpStatusCode, String message) {
    this.status = status;
    this.httpStatusCode = httpStatusCode;
    this.message = message;
  }
}
