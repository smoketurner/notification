package com.smoketurner.notification.application.integration;

import static org.assertj.core.api.Assertions.assertThat;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.application.NotificationApplication;
import com.smoketurner.notification.application.config.NotificationConfiguration;

public class NotificationsIT {

    private static final String TEST_USER = "test";

    @ClassRule
    public static final DropwizardAppRule<NotificationConfiguration> RULE = new DropwizardAppRule<NotificationConfiguration>(
            NotificationApplication.class,
            ResourceHelpers.resourceFilePath("notification-test.yml"));

    private Client client;

    @Before
    public void setUp() throws Exception {
        client = new JerseyClientBuilder(RULE.getEnvironment())
                .build("test client");
    }

    @After
    public void tearDown() throws Exception {
        client.close();
    }

    @Test
    public void testCreateNotification() throws Exception {
        final Notification notification = Notification.builder()
                .withCategory("test-category")
                .withMessage("this is only a test").build();

        final Response response = client
                .target(String.format(
                        "http://localhost:%d/v1/notifications/%s",
                        RULE.getLocalPort(), TEST_USER)).request()
                .post(Entity.json(notification));

        final Notification actual = response.readEntity(Notification.class);

        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getLocation().getPath()).isEqualTo(
                "/v1/notifications/" + TEST_USER);
        assertThat(actual.getCategory()).isEqualTo(notification.getCategory());
        assertThat(actual.getMessage()).isEqualTo(notification.getMessage());
        assertThat(actual.getId().isPresent()).isTrue();
        assertThat(actual.getCreatedAt().isPresent()).isTrue();
    }

    @Test
    public void testFetchNotifications() {
        final Response response = client
                .target(String.format(
                        "http://localhost:%d/v1/notifications/%s",
                        RULE.getLocalPort(), TEST_USER)).request().get();

        if (response.getStatus() != 404) {
            assertThat(response.getStatus()).isEqualTo(200);
            final List<Notification> actual = response
                    .readEntity(new GenericType<List<Notification>>() {
                    });
            assertThat(actual.size()).isGreaterThan(0);
            final Notification first = actual.get(0);
            assertThat(first.getId().isPresent()).isTrue();
            assertThat(first.getCreatedAt().isPresent()).isTrue();
        }
    }

    @Test
    public void testDeleteAllNotifications() {
        final Response response = client
                .target(String.format(
                        "http://localhost:%d/v1/notifications/%s",
                        RULE.getLocalPort(), TEST_USER)).request().delete();

        assertThat(response.getStatus()).isEqualTo(204);
    }
}
