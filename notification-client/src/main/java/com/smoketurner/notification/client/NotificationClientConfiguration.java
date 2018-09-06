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
package com.smoketurner.notification.client;

import io.dropwizard.client.JerseyClientConfiguration;
import java.net.URI;
import org.hibernate.validator.constraints.NotEmpty;

public class NotificationClientConfiguration extends JerseyClientConfiguration {

  @NotEmpty private String uri = "http://127.0.0.1:8080/api";

  public URI getUri() {
    return URI.create(uri);
  }

  public void setUri(final String uri) {
    this.uri = uri;
  }
}
