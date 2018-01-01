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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.application.core.UserNotifications;
import com.smoketurner.notification.application.exceptions.NotificationExceptionMapper;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import com.smoketurner.notification.application.store.NotificationStore;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.errors.ErrorMessage;
import io.dropwizard.jersey.filter.CharsetUtf8Filter;
import io.dropwizard.jersey.validation.ValidationErrorMessage;
import io.dropwizard.testing.junit.ResourceTestRule;

public class NotificationResourceTest {

    private static final ObjectMapper MAPPER = Jackson.newObjectMapper();
    private static final NotificationStore store = mock(
            NotificationStore.class);

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new NotificationResource(store))
            .addProvider(new CharsetUtf8Filter())
            .addProvider(new NotificationExceptionMapper()).build();

    @After
    public void tearDown() {
        reset(store);
    }

    @Test
    public void testFetch() throws Exception {
        final ImmutableSortedSet<Notification> expected = ImmutableSortedSet
                .of(createNotification(1L));
        final UserNotifications notifications = new UserNotifications(expected);
        when(store.fetch("test")).thenReturn(Optional.of(notifications));
        when(store.skip(notifications.getNotifications(), 1L, true, 20))
                .thenReturn(expected);

        final Response response = resources.client()
                .target("/v1/notifications/test")
                .request(MediaType.APPLICATION_JSON).get();
        final List<Notification> actual = response
                .readEntity(new GenericType<List<Notification>>() {
                });

        verify(store).fetch("test");
        verify(store).skip(notifications.getNotifications(), 1L, true, 20);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE))
                .isEqualTo(MediaType.APPLICATION_JSON + ";charset=UTF-8");
        assertThat(response.getHeaderString("Accept-Ranges")).isEqualTo("id");
        assertThat(response.getHeaderString("Content-Range"))
                .isEqualTo("id 1..1");
        assertThat(response.getHeaderString("Next-Range")).isNull();
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    @Test
    public void testFetchJSONP() throws Exception {
        final ZonedDateTime now = ZonedDateTime.now(Clock.systemUTC());
        final Notification notification = Notification.builder()
                .fromNotification(createNotification(1L)).withCreatedAt(now)
                .build();
        final ImmutableSortedSet<Notification> expected = ImmutableSortedSet
                .of(notification);
        final UserNotifications notifications = new UserNotifications(expected);
        when(store.fetch("test")).thenReturn(Optional.of(notifications));
        when(store.skip(notifications.getNotifications(), 1L, true, 20))
                .thenReturn(expected);

        final Response response = resources.client()
                .target("/v1/notifications/test")
                .request("application/javascript").get();
        final String actual = response.readEntity(String.class);

        verify(store).fetch("test");
        verify(store).skip(notifications.getNotifications(), 1L, true, 20);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE))
                .isEqualTo("application/javascript;charset=UTF-8");
        assertThat(response.getHeaderString("Accept-Ranges")).isEqualTo("id");
        assertThat(response.getHeaderString("Content-Range"))
                .isEqualTo("id 1..1");
        assertThat(response.getHeaderString("Next-Range")).isNull();
        assertThat(actual).isEqualTo(
                "callback([" + MAPPER.writeValueAsString(notification) + "])");
    }

    @Test
    public void testFetchRange() throws Exception {
        final ImmutableList.Builder<Notification> builder = ImmutableList
                .builder();
        for (long i = 20; i > 0; i--) {
            builder.add(createNotification(i));
        }
        final List<Notification> all = builder.build();

        final Set<Notification> expected = ImmutableSortedSet
                .of(createNotification(19L), createNotification(18L));

        final UserNotifications notifications = new UserNotifications(all);
        when(store.fetch("test")).thenReturn(Optional.of(notifications));
        when(store.skip(notifications.getNotifications(), 20L, false, 2))
                .thenReturn(expected);

        final Response response = resources.client()
                .target("/v1/notifications/test")
                .request(MediaType.APPLICATION_JSON)
                .header("Range", "id ]20..; max=2").get();
        final List<Notification> actual = response
                .readEntity(new GenericType<List<Notification>>() {
                });

        verify(store).fetch("test");
        verify(store).skip(notifications.getNotifications(), 20L, false, 2);
        assertThat(response.getStatus()).isEqualTo(206);
        assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE))
                .isEqualTo(MediaType.APPLICATION_JSON + ";charset=UTF-8");
        assertThat(response.getHeaderString("Accept-Ranges")).isEqualTo("id");
        assertThat(response.getHeaderString("Content-Range"))
                .isEqualTo("id 19..18");
        assertThat(response.getHeaderString("Next-Range"))
                .isEqualTo("id ]18..; max=2");
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    @Test
    public void testFetchRangeEmpty() throws Exception {
        final ImmutableList.Builder<Notification> builder = ImmutableList
                .builder();
        for (long i = 30; i > 0; i--) {
            builder.add(createNotification(i));
        }
        final List<Notification> all = builder.build();

        final List<Notification> expected = all.subList(0, 20);

        final UserNotifications notifications = new UserNotifications(all);
        when(store.fetch("test")).thenReturn(Optional.of(notifications));
        when(store.skip(notifications.getNotifications(), 30L, true, 20))
                .thenReturn(expected);

        final Response response = resources.client()
                .target("/v1/notifications/test")
                .request(MediaType.APPLICATION_JSON).header("Range", "").get();
        final List<Notification> actual = response
                .readEntity(new GenericType<List<Notification>>() {
                });

        verify(store).fetch("test");
        verify(store).skip(notifications.getNotifications(), 30L, true, 20);
        assertThat(response.getStatus()).isEqualTo(206);
        assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE))
                .isEqualTo(MediaType.APPLICATION_JSON + ";charset=UTF-8");
        assertThat(response.getHeaderString("Accept-Ranges")).isEqualTo("id");
        assertThat(response.getHeaderString("Content-Range"))
                .isEqualTo("id 30..11");
        assertThat(response.getHeaderString("Next-Range"))
                .isEqualTo("id ]11..; max=20");
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    @Test
    public void testFetchRangeInvalidId() throws Exception {
        final ImmutableList.Builder<Notification> builder = ImmutableList
                .builder();
        for (long i = 30; i > 0; i--) {
            builder.add(createNotification(i));
        }
        final List<Notification> all = builder.build();

        final List<Notification> expected = all.subList(0, 20);

        final UserNotifications notifications = new UserNotifications(all);
        when(store.fetch("test")).thenReturn(Optional.of(notifications));
        when(store.skip(notifications.getNotifications(), 30L, true, 20))
                .thenReturn(expected);

        final Response response = resources.client()
                .target("/v1/notifications/test")
                .request(MediaType.APPLICATION_JSON)
                .header("Range", "id 1000..").get();
        final List<Notification> actual = response
                .readEntity(new GenericType<List<Notification>>() {
                });

        verify(store).fetch("test");
        verify(store).skip(notifications.getNotifications(), 30L, true, 20);
        assertThat(response.getStatus()).isEqualTo(206);
        assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE))
                .isEqualTo(MediaType.APPLICATION_JSON + ";charset=UTF-8");
        assertThat(response.getHeaderString("Accept-Ranges")).isEqualTo("id");
        assertThat(response.getHeaderString("Content-Range"))
                .isEqualTo("id 30..11");
        assertThat(response.getHeaderString("Next-Range"))
                .isEqualTo("id ]11..; max=20");
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    @Test
    public void testFetchRangeMax() throws Exception {
        final ImmutableList.Builder<Notification> builder = ImmutableList
                .builder();
        for (long i = 20; i > 0; i--) {
            builder.add(createNotification(i));
        }
        final List<Notification> all = builder.build();

        final List<Notification> expected = all.subList(0, 3);

        final UserNotifications notifications = new UserNotifications(all);
        when(store.fetch("test")).thenReturn(Optional.of(notifications));
        when(store.skip(notifications.getNotifications(), 20L, true, 3))
                .thenReturn(expected);

        final Response response = resources.client()
                .target("/v1/notifications/test")
                .request(MediaType.APPLICATION_JSON).header("Range", "id;max=3")
                .get();
        final List<Notification> actual = response
                .readEntity(new GenericType<List<Notification>>() {
                });

        verify(store).fetch("test");
        verify(store).skip(notifications.getNotifications(), 20L, true, 3);
        assertThat(response.getStatus()).isEqualTo(206);
        assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE))
                .isEqualTo(MediaType.APPLICATION_JSON + ";charset=UTF-8");
        assertThat(response.getHeaderString("Accept-Ranges")).isEqualTo("id");
        assertThat(response.getHeaderString("Content-Range"))
                .isEqualTo("id 20..18");
        assertThat(response.getHeaderString("Next-Range"))
                .isEqualTo("id ]18..; max=3");
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    @Test
    public void testFetchNotFound() throws Exception {
        when(store.fetch("test"))
                .thenReturn(Optional.<UserNotifications>empty());

        final Response response = resources.client()
                .target("/v1/notifications/test")
                .request(MediaType.APPLICATION_JSON).get();
        final ErrorMessage actual = response.readEntity(ErrorMessage.class);

        verify(store).fetch("test");
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(actual.getCode()).isEqualTo(404);
    }

    @Test
    public void testFetchException() throws Exception {
        when(store.fetch("test")).thenThrow(new NotificationStoreException());

        final Response response = resources.client()
                .target("/v1/notifications/test")
                .request(MediaType.APPLICATION_JSON).get();
        final ErrorMessage actual = response.readEntity(ErrorMessage.class);

        verify(store).fetch("test");
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(actual.getCode()).isEqualTo(500);
    }

    @Test
    public void testStore() throws Exception {
        final Notification expected = Notification
                .builder("test-category", "testing 1 2 3").withId(1L)
                .withCreatedAt(ZonedDateTime.now(Clock.systemUTC())).build();

        final Notification notification = Notification
                .builder("test-category", "testing 1 2 3").build();

        when(store.store("test", notification)).thenReturn(expected);

        final Response response = resources.client()
                .target("/v1/notifications/test")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(notification));
        final Notification actual = response.readEntity(Notification.class);

        verify(store).store("test", notification);
        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getLocation().getPath())
                .isEqualTo("/v1/notifications/test");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testStoreException() throws Exception {
        final Notification notification = Notification
                .builder("test-category", "testing 1 2 3").build();
        when(store.store("test", notification))
                .thenThrow(new NotificationStoreException());

        final Response response = resources.client()
                .target("/v1/notifications/test")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(notification));
        final ErrorMessage actual = response.readEntity(ErrorMessage.class);

        verify(store).store("test", notification);
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(actual.getCode()).isEqualTo(500);
    }

    @Test
    public void testStoreEmptyCategory() throws Exception {
        final Notification notification = Notification
                .builder("", "testing 1 2 3").build();

        final Response response = resources.client()
                .target("/v1/notifications/test")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(notification));

        verify(store, never()).store(anyString(), any(Notification.class));
        assertThat(response.getStatus()).isEqualTo(422);

        final ValidationErrorMessage msg = response
                .readEntity(ValidationErrorMessage.class);
        assertThat(msg.getErrors()).containsOnly("category may not be empty");
    }

    @Test
    public void testStoreMissingMessage() throws Exception {
        final Notification notification = Notification.builder("test-category")
                .build();

        final Response response = resources.client()
                .target("/v1/notifications/test")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(notification));

        verify(store, never()).store(anyString(), any(Notification.class));
        assertThat(response.getStatus()).isEqualTo(422);

        final ValidationErrorMessage msg = response
                .readEntity(ValidationErrorMessage.class);
        assertThat(msg.getErrors()).containsOnly("message may not be empty");
    }

    @Test
    public void testStoreEmptyMessage() throws Exception {
        final Notification notification = Notification
                .builder("test-category", "").build();

        final Response response = resources.client()
                .target("/v1/notifications/test")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(notification));

        verify(store, never()).store(anyString(), any(Notification.class));
        assertThat(response.getStatus()).isEqualTo(422);

        final ValidationErrorMessage msg = response
                .readEntity(ValidationErrorMessage.class);
        assertThat(msg.getErrors()).containsOnly("message may not be empty");
    }

    @Test
    public void testStoreNull() throws Exception {
        final Response response = resources.client()
                .target("/v1/notifications/test")
                .request(MediaType.APPLICATION_JSON).post(null);

        verify(store, never()).store(anyString(), any(Notification.class));
        assertThat(response.getStatus()).isEqualTo(422);

        final ValidationErrorMessage msg = response
                .readEntity(ValidationErrorMessage.class);
        assertThat(msg.getErrors())
                .containsOnly("The request body may not be null");
    }

    @Test
    public void testRemove() throws Exception {
        final Response response = resources.client()
                .target("/v1/notifications/test").request().delete();

        verify(store).removeAll("test");
        assertThat(response.getStatus()).isEqualTo(204);
    }

    @Test
    public void testRemoveIds() throws Exception {
        final Response response = resources.client()
                .target("/v1/notifications/test?ids=1,2,asdf,3").request()
                .delete();

        verify(store).remove("test", ImmutableSet.of(1L, 2L, 3L));
        verify(store, never()).removeAll(anyString());
        assertThat(response.getStatus()).isEqualTo(204);
    }

    private Notification createNotification(final long id) {
        return Notification.builder().withId(id).build();
    }
}
