/*
 * Copyright © 2019 Smoke Turner, LLC (github@smoketurner.com)
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
package com.smoketurner.notification.application.resources;

import com.codahale.metrics.annotation.Timed;
import com.smoketurner.notification.api.Rule;
import com.smoketurner.notification.application.exceptions.NotificationException;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import com.smoketurner.notification.application.store.RuleStore;
import io.dropwizard.jersey.caching.CacheControl;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;

@Path("/v1/rules")
public class RuleResource {

  private final RuleStore store;

  /**
   * Constructor
   *
   * @param store Rule data store
   */
  public RuleResource(final RuleStore store) {
    this.store = Objects.requireNonNull(store);
  }

  @GET
  @JSONP
  @Timed
  @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
  @CacheControl(mustRevalidate = true, noCache = true, noStore = true)
  public Response fetch() {

    final Optional<Map<String, Rule>> rules;
    try {
      rules = store.fetch();
    } catch (NotificationStoreException e) {
      throw new NotificationException(
          Response.Status.INTERNAL_SERVER_ERROR, "Unable to fetch rules", e);
    }

    if (!rules.isPresent()) {
      throw new NotificationException(Response.Status.NOT_FOUND, "No rules found");
    }

    return Response.ok(rules.get()).build();
  }

  @PUT
  @Timed
  @Path("/{category}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response store(
      @PathParam("category") final String category, @NotNull @Valid final Rule rule) {

    if (!rule.isValid()) {
      throw new NotificationException(
          Response.Status.BAD_REQUEST,
          "Rule must contain at least one of: max_size, max_duration, or match_on");
    }

    try {
      store.store(category, rule);
    } catch (NotificationStoreException e) {
      throw new NotificationException(
          Response.Status.INTERNAL_SERVER_ERROR, "Unable to store rule", e);
    }

    return Response.noContent().build();
  }

  @DELETE
  @Timed
  @Path("/{category}")
  public Response delete(@PathParam("category") final String category) {

    try {
      store.remove(category);
    } catch (NotificationStoreException e) {
      throw new NotificationException(
          Response.Status.INTERNAL_SERVER_ERROR, "Unable to remove rule", e);
    }
    return Response.noContent().build();
  }

  @DELETE
  @Timed
  public Response delete() {

    try {
      store.removeAll();
    } catch (NotificationStoreException e) {
      throw new NotificationException(
          Response.Status.INTERNAL_SERVER_ERROR, "Unable to remove rules", e);
    }
    return Response.noContent().build();
  }
}
