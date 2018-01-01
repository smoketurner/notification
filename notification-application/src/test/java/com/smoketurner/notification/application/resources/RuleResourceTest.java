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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.smoketurner.notification.api.Rule;
import com.smoketurner.notification.application.exceptions.NotificationExceptionMapper;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import com.smoketurner.notification.application.store.RuleStore;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.errors.ErrorMessage;
import io.dropwizard.jersey.filter.CharsetUtf8Filter;
import io.dropwizard.testing.junit.ResourceTestRule;

public class RuleResourceTest {

    private static final ObjectMapper MAPPER = Jackson.newObjectMapper();
    private static final RuleStore store = mock(RuleStore.class);

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new RuleResource(store))
            .addProvider(new CharsetUtf8Filter())
            .addProvider(new NotificationExceptionMapper()).build();

    @After
    public void tearDown() {
        reset(store);
    }

    @Test
    public void testFetch() throws Exception {
        final Map<String, Rule> expected = ImmutableMap.of("follow",
                Rule.builder().withMaxSize(3).build());

        when(store.fetch()).thenReturn(Optional.of(expected));

        final Response response = resources.client().target("/v1/rules")
                .request(MediaType.APPLICATION_JSON).get();
        final Map<String, Rule> actual = response
                .readEntity(new GenericType<Map<String, Rule>>() {
                });

        verify(store).fetch();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE))
                .isEqualTo(MediaType.APPLICATION_JSON + ";charset=UTF-8");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testFetchJSONP() throws Exception {
        final Map<String, Rule> expected = ImmutableMap.of("follow",
                Rule.builder().withMaxSize(3).build());

        when(store.fetch()).thenReturn(Optional.of(expected));

        final Response response = resources.client().target("/v1/rules")
                .request("application/javascript").get();
        final String actual = response.readEntity(String.class);

        verify(store).fetch();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE))
                .isEqualTo("application/javascript;charset=UTF-8");
        assertThat(actual).isEqualTo(
                "callback(" + MAPPER.writeValueAsString(expected) + ")");
    }

    @Test
    public void testFetchNotFound() throws Exception {
        when(store.fetch()).thenReturn(Optional.empty());

        final Response response = resources.client().target("/v1/rules")
                .request(MediaType.APPLICATION_JSON).get();
        final ErrorMessage actual = response.readEntity(ErrorMessage.class);

        verify(store).fetch();
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(actual.getCode()).isEqualTo(404);
    }

    @Test
    public void testFetchException() throws Exception {
        when(store.fetch()).thenThrow(new NotificationStoreException());

        final Response response = resources.client().target("/v1/rules")
                .request(MediaType.APPLICATION_JSON).get();
        final ErrorMessage actual = response.readEntity(ErrorMessage.class);

        verify(store).fetch();
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(actual.getCode()).isEqualTo(500);
    }

    @Test
    public void testStore() throws Exception {
        final Rule rule = Rule.builder().withMaxSize(3).build();

        final Response response = resources.client().target("/v1/rules/follow")
                .request(MediaType.APPLICATION_JSON).put(Entity.json(rule));

        verify(store).store("follow", rule);
        assertThat(response.getStatus()).isEqualTo(204);
    }

    @Test
    public void testStoreInvalidRule() throws Exception {
        final Rule rule = Rule.builder().build();

        final Response response = resources.client().target("/v1/rules/follow")
                .request(MediaType.APPLICATION_JSON).put(Entity.json(rule));
        final ErrorMessage actual = response.readEntity(ErrorMessage.class);

        verify(store, never()).store(anyString(), any(Rule.class));
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(actual.getCode()).isEqualTo(400);
    }

    @Test
    public void testStoreException() throws Exception {
        doThrow(new NotificationStoreException()).when(store).store(anyString(),
                any(Rule.class));

        final Rule rule = Rule.builder().withMaxSize(3).build();

        final Response response = resources.client().target("/v1/rules/follow")
                .request(MediaType.APPLICATION_JSON).put(Entity.json(rule));
        final ErrorMessage actual = response.readEntity(ErrorMessage.class);

        verify(store).store("follow", rule);
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(actual.getCode()).isEqualTo(500);
    }

    @Test
    public void testDelete() throws Exception {
        final Response response = resources.client().target("/v1/rules/follow")
                .request(MediaType.APPLICATION_JSON).delete();
        verify(store).remove("follow");
        assertThat(response.getStatus()).isEqualTo(204);
    }

    @Test
    public void testDeleteException() throws Exception {
        doThrow(new NotificationStoreException()).when(store)
                .remove(anyString());

        final Response response = resources.client().target("/v1/rules/follow")
                .request(MediaType.APPLICATION_JSON).delete();
        final ErrorMessage actual = response.readEntity(ErrorMessage.class);

        verify(store).remove("follow");
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(actual.getCode()).isEqualTo(500);
    }

    @Test
    public void testDeleteAll() throws Exception {
        final Response response = resources.client().target("/v1/rules")
                .request(MediaType.APPLICATION_JSON).delete();
        verify(store).removeAll();
        assertThat(response.getStatus()).isEqualTo(204);
    }
}
