package com.smoketurner.notification.client.integration;

import java.net.URI;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.client.JerseyClientBuilder;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.client.NotificationClient;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.jersey.validation.Validators;

public class NotificationGenerator {

    private static final String USERNAME = "test";
    private static final int MAX_NOTIFICATIONS = 3000;
    private static final int NUM_THREADS = 3;
    private static final Random RANDOM = new Random();
    private static final Map<Integer, String> USERS = ImmutableMap.of(0,
            "Blade", 1, "Spiderman", 2, "Batman", 3, "Robin", 4, "Colossus");

    public static void main(String[] args) throws Exception {
        final MetricRegistry registry = new MetricRegistry();
        final URI uri = UriBuilder.fromUri("http://localhost:8080").build();
        final Client jerseyClient = new JerseyClientBuilder()
                .register(new JacksonMessageBodyProvider(
                        Jackson.newObjectMapper(), Validators.newValidator()))
                .build();
        final NotificationClient client = new NotificationClient(registry,
                jerseyClient, uri);

        final CountDownLatch latch = new CountDownLatch(NUM_THREADS);

        // new-follower
        final Thread follower = new Thread(() -> {
            for (int i = 0; i < MAX_NOTIFICATIONS / NUM_THREADS; i++) {
                final int userId = RANDOM.nextInt(USERS.size());
                final String message = String.format("%s is now following you",
                        USERS.get(userId));

                final Notification notification = Notification.builder()
                        .withCategory("new-follower")
                        .withMessage(message).withProperties(ImmutableMap
                                .of("follower_id", String.valueOf(userId)))
                        .build();

                client.store(USERNAME, notification);
            }
            latch.countDown();
        });
        follower.setDaemon(true);
        follower.start();

        // like
        final Thread like = new Thread(() -> {
            for (int i = 0; i < MAX_NOTIFICATIONS / NUM_THREADS; i++) {
                final int userId = RANDOM.nextInt(USERS.size());
                final String message = String.format("%s liked your post",
                        USERS.get(userId));
                final int messageId = RANDOM.nextInt(5);

                final Notification notification = Notification.builder()
                        .withCategory("like").withMessage(message)
                        .withProperties(ImmutableMap.of("message_id",
                                String.valueOf(messageId), "liker_id",
                                String.valueOf(userId)))
                        .build();

                client.store(USERNAME, notification);
            }
            latch.countDown();
        });
        like.setDaemon(true);
        like.start();

        // mention
        final Thread mention = new Thread(() -> {
            for (int i = 0; i < MAX_NOTIFICATIONS / NUM_THREADS; i++) {
                final int userId = RANDOM.nextInt(USERS.size());
                final String message = String.format(
                        "%s mentioned you in a post", USERS.get(userId));
                final int messageId = RANDOM.nextInt(5);

                final Notification notification = Notification.builder()
                        .withCategory("mention").withMessage(message)
                        .withProperties(ImmutableMap.of("message_id",
                                String.valueOf(messageId), "user_id",
                                String.valueOf(userId)))
                        .build();

                client.store(USERNAME, notification);
            }
            latch.countDown();
        });
        mention.setDaemon(true);
        mention.start();

        latch.await();
        client.close();
    }
}
