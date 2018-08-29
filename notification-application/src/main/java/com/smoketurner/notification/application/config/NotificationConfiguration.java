/*
 * Copyright © 2018 Smoke Turner, LLC (contact@smoketurner.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.notification.application.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smoketurner.dropwizard.riak.RiakFactory;
import io.dropwizard.Configuration;
import io.dropwizard.util.Duration;
import io.dropwizard.validation.MinDuration;
import java.util.concurrent.TimeUnit;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class NotificationConfiguration extends Configuration {

  @NotNull
  @MinDuration(value = 1, unit = TimeUnit.SECONDS)
  private Duration ruleCacheTimeout = Duration.seconds(30);

  @NotNull
  @MinDuration(value = 1, unit = TimeUnit.MILLISECONDS)
  private Duration riakTimeout = Duration.seconds(60);

  @NotNull
  @MinDuration(value = 1, unit = TimeUnit.MILLISECONDS)
  private Duration riakRequestTimeout = Duration.seconds(5);

  @Valid @NotNull @JsonProperty private final RiakFactory riak = new RiakFactory();

  @JsonProperty
  public Duration getRiakTimeout() {
    return riakTimeout;
  }

  @JsonProperty
  public void setRiakTimeout(final Duration timeout) {
    this.riakTimeout = timeout;
  }

  @JsonProperty
  public Duration getRiakRequestTimeout() {
    return riakRequestTimeout;
  }

  @JsonProperty
  public void setRiakRequestTimeout(final Duration timeout) {
    this.riakRequestTimeout = timeout;
  }

  @JsonProperty
  public Duration getRuleCacheTimeout() {
    return ruleCacheTimeout;
  }

  @JsonProperty
  public void setRuleCacheTimeout(final Duration timeout) {
    this.ruleCacheTimeout = timeout;
  }

  @JsonProperty
  public RiakFactory getRiak() {
    return riak;
  }
}
