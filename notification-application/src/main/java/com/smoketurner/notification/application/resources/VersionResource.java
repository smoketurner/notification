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
package com.smoketurner.notification.application.resources;

import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.jersey.caching.CacheControl;
import java.util.Objects;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/version")
public class VersionResource {

  private final String version;

  /** Constructor */
  public VersionResource() {
    version = getClass().getPackage().getImplementationVersion();
  }

  /**
   * Constructor
   *
   * @param version Version to expose in the endpoint
   */
  @VisibleForTesting
  public VersionResource(final String version) {
    this.version = Objects.requireNonNull(version);
  }

  @GET
  @CacheControl(mustRevalidate = true, noCache = true, noStore = true)
  public Response getVersion() {
    return Response.ok(version).type(MediaType.TEXT_PLAIN).build();
  }
}
