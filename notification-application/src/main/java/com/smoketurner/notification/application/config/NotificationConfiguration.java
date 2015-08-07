/**
 * Copyright 2015 Smoke Turner, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.smoketurner.notification.application.config;

import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.eclipse.jetty.servlets.CrossOriginFilter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.smoketurner.notification.application.core.Rule;

public class NotificationConfiguration extends Configuration {

  @NotNull
  private Map<String, Rule> rules = Maps.newHashMap();

  @NotNull
  private Set<String> allowedHeaders = ImmutableSet.of("X-Requested-With", "Content-Type",
      "Accept", "Origin", "Range");

  @Valid
  @NotNull
  @JsonProperty
  public final SwaggerBundleConfiguration swagger = new SwaggerBundleConfiguration();

  @Valid
  @NotNull
  @JsonProperty
  private final RiakConfiguration riak = new RiakConfiguration();

  @Valid
  @NotNull
  @JsonProperty
  private final SnowizardConfiguration snowizard = new SnowizardConfiguration();

  @JsonProperty
  public Map<String, Rule> getRules() {
    return rules;
  }

  @JsonProperty
  public void setRules(@Nonnull final Map<String, Rule> rules) {
    this.rules = rules;
  }

  @JsonProperty
  public Set<String> getAllowedHeaders() {
    return allowedHeaders;
  }

  @JsonProperty
  public void setAllowedHeaders(@Nonnull final Set<String> allowedHeaders) {
    this.allowedHeaders = allowedHeaders;
  }

  @JsonProperty
  public SwaggerBundleConfiguration getSwagger() {
    return swagger;
  }

  @JsonProperty
  public RiakConfiguration getRiak() {
    return riak;
  }

  @JsonProperty
  public SnowizardConfiguration getSnowizard() {
    return snowizard;
  }

  /**
   * Registers the Jetty CrossOriginFilter to support CORS requests.
   *
   * @param environment Environment object
   */
  public void registerCrossOriginFilter(@Nonnull final Environment environment) {
    final FilterRegistration.Dynamic filter =
        environment.servlets().addFilter("CrossOriginFilter", CrossOriginFilter.class);
    filter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
    filter.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM,
        Joiner.on(',').join(allowedHeaders));
  }
}
