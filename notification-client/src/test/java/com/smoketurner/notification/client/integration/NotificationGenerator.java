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
package com.smoketurner.notification.client.integration;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.client.NotificationClient;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import java.net.URI;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.client.JerseyClientBuilder;

public class NotificationGenerator {

  private static final String USERNAME = "test";
  private static final int MAX_NOTIFICATIONS = 3000;
  private static final int NUM_THREADS = 3;
  private static final Random RANDOM = new Random();
  private static final Map<Integer, String> USERS =
      ImmutableMap.of(0, "Blade", 1, "Spiderman", 2, "Batman", 3, "Robin", 4, "Colossus");

  public static void main(String[] args) throws Exception {
    final MetricRegistry registry = new MetricRegistry();
    final URI uri = UriBuilder.fromUri("http://localhost:8080").build();
    final Client jerseyClient =
        new JerseyClientBuilder()
            .register(new JacksonMessageBodyProvider(Jackson.newObjectMapper()))
            .build();
    final NotificationClient client = new NotificationClient(registry, jerseyClient, uri);

    final CountDownLatch latch = new CountDownLatch(NUM_THREADS);

    final ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

    // new-follower
    executor.execute(
        () -> {
          final int count = MAX_NOTIFICATIONS / NUM_THREADS;
          System.out.println("Starting to send " + count + " new-followers");

          int userId;
          String message;
          Notification notification;
          for (int i = 0; i < count; i++) {
            userId = RANDOM.nextInt(USERS.size());
            message = String.format("%s is now following you", USERS.get(userId));

            notification =
                Notification.builder("new-follower", message)
                    .withProperties(ImmutableMap.of("follower_id", String.valueOf(userId)))
                    .build();

            client.store(USERNAME, notification);
          }
          System.out.println("Finished creating " + count + " new-followers");
          latch.countDown();
        });

    // like
    executor.execute(
        () -> {
          final int count = MAX_NOTIFICATIONS / NUM_THREADS;
          System.out.println("Starting to send " + count + " likes");

          int userId;
          String message;
          int messageId;
          Notification notification;
          for (int i = 0; i < count; i++) {
            userId = RANDOM.nextInt(USERS.size());
            message = String.format("%s liked your post", USERS.get(userId));
            messageId = RANDOM.nextInt(5);

            notification =
                Notification.builder("like", message)
                    .withProperties(
                        ImmutableMap.of(
                            "message_id",
                            String.valueOf(messageId),
                            "liker_id",
                            String.valueOf(userId)))
                    .build();

            client.store(USERNAME, notification);
          }
          System.out.println("Finished creating " + count + " likes");
          latch.countDown();
        });

    // mention
    executor.execute(
        () -> {
          final int count = MAX_NOTIFICATIONS / NUM_THREADS;
          System.out.println("Starting to send " + count + " mentions");

          int userId;
          String message;
          int messageId;
          Notification notification;
          for (int i = 0; i < count; i++) {
            userId = RANDOM.nextInt(USERS.size());
            message = String.format("%s mentioned you in a post", USERS.get(userId));
            messageId = RANDOM.nextInt(5);

            notification =
                Notification.builder("mention", message)
                    .withProperties(
                        ImmutableMap.of(
                            "message_id",
                            String.valueOf(messageId),
                            "user_id",
                            String.valueOf(userId)))
                    .build();

            client.store(USERNAME, notification);
          }
          System.out.println("Finished creating " + count + " mentions");
          latch.countDown();
        });

    latch.await();
    client.close();
  }
}
