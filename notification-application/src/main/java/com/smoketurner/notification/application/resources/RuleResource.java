/**
 * Copyright 2018 Smoke Turner, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.notification.application.resources;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
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
import com.codahale.metrics.annotation.Timed;
import com.smoketurner.notification.api.Rule;
import com.smoketurner.notification.application.exceptions.NotificationException;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import com.smoketurner.notification.application.store.RuleStore;
import io.dropwizard.jersey.caching.CacheControl;
import io.dropwizard.jersey.errors.ErrorMessage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/v1/rules")
@Api(value = "rules")
public class RuleResource {

    private final RuleStore store;

    /**
     * Constructor
     *
     * @param store
     *            Rule data store
     */
    public RuleResource(@Nonnull final RuleStore store) {
        this.store = Objects.requireNonNull(store);
    }

    @GET
    @JSONP
    @Timed
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    @CacheControl(mustRevalidate = true, noCache = true, noStore = true)
    @ApiOperation(value = "Fetch Rules", notes = "Return all of the rules",
                  responseContainer = "Map", response = Rule.class)
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Unable to fetch rules",
                         response = ErrorMessage.class),
            @ApiResponse(code = 404, message = "No rules found",
                         response = ErrorMessage.class) })
    public Response fetch() {

        final Optional<Map<String, Rule>> rules;
        try {
            rules = store.fetch();
        } catch (NotificationStoreException e) {
            throw new NotificationException(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    "Unable to fetch rules", e);
        }

        if (!rules.isPresent()) {
            throw new NotificationException(Response.Status.NOT_FOUND,
                    "No rules found");
        }

        return Response.ok(rules.get()).build();
    }

    @PUT
    @Timed
    @Path("/{category}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Store Rule", notes = "Add a new rule",
                  response = Rule.class)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Successfully stored rule"),
            @ApiResponse(code = 500, message = "Unable to store rule",
                         response = ErrorMessage.class) })
    public Response store(
            @ApiParam(value = "category",
                      required = true) @PathParam("category") final String category,
            @ApiParam(value = "rule",
                      required = true) @NotNull @Valid final Rule rule) {

        if (!rule.isValid()) {
            throw new NotificationException(Response.Status.BAD_REQUEST,
                    "Rule must contain at least one of: max_size, max_duration, or match_on");
        }

        try {
            store.store(category, rule);
        } catch (NotificationStoreException e) {
            throw new NotificationException(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    "Unable to store rule", e);
        }

        return Response.noContent().build();
    }

    @DELETE
    @Timed
    @Path("/{category}")
    @ApiOperation(value = "Delete Rule", notes = "Delete a rule")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Successfully deleted rule"),
            @ApiResponse(code = 500, message = "Unable to delete rule",
                         response = ErrorMessage.class) })
    public Response delete(
            @ApiParam(value = "category",
                      required = true) @PathParam("category") final String category) {

        try {
            store.remove(category);
        } catch (NotificationStoreException e) {
            throw new NotificationException(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    "Unable to remove rule", e);
        }
        return Response.noContent().build();
    }

    @DELETE
    @Timed
    @ApiOperation(value = "Delete All Rules", notes = "Delete all rules")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Successfully deleted rules"),
            @ApiResponse(code = 500, message = "Unable to delete rules",
                         response = ErrorMessage.class) })
    public Response delete() {

        store.removeAll();
        return Response.noContent().build();
    }
}
