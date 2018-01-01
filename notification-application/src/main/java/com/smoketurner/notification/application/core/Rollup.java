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
package com.smoketurner.notification.application.core;

import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.api.Rule;

public class Rollup {

    private final Map<String, Rule> rules;
    private final TreeSet<Matcher> matchers = new TreeSet<>();

    /**
     * Constructor
     *
     * @param rules
     *            Map of rules
     */
    public Rollup(@Nonnull final Map<String, Rule> rules) {
        this.rules = Objects.requireNonNull(rules, "rules == null");
    }

    /**
     * Iterates over the notifications and uses the {@link Rule} and
     * {@link Matcher} objects to roll up the notifications based on the rules.
     * 
     * @param notifications
     *            Notifications to roll up
     * @return Rolled up notifications
     */
    public Stream<Notification> rollup(
            @Nonnull final Stream<Notification> notifications) {
        Objects.requireNonNull(notifications, "notifications == null");

        if (rules.isEmpty()) {
            return notifications;
        }

        final TreeSet<Notification> rollups = new TreeSet<>();

        notifications.forEachOrdered(notification -> {
            final Rule rule = rules.get(notification.getCategory());

            // If the notification category doesn't match any rule categories,
            // add the notification as-is to the list of rollups.
            if (rule == null || !rule.isValid()) {
                rollups.add(notification);
            } else if (matchers.isEmpty()) {
                // If we don't have any matchers yet, add the first one
                matchers.add(new Matcher(rule, notification));
            } else {
                // Loop through the existing matchers to see if this
                // notification falls into any previous rollups
                boolean matched = false;
                for (final Matcher matcher : matchers) {
                    if (matcher.test(notification)) {
                        matched = true;

                        // if the matcher is now full, add it to the rollups and
                        // remove it from the available matchers which still
                        // have empty space.
                        if (matcher.isFull()) {
                            matchers.remove(matcher);
                            rollups.add(matcher.getNotification());
                        }
                        break;
                    }
                }

                // If the notification didn't match any existing rollups, add it
                // as a new matcher
                if (!matched) {
                    matchers.add(new Matcher(rule, notification));
                }
            }
        });

        // Pull out the rolled up notifications out of the matchers
        for (final Matcher match : matchers) {
            rollups.add(match.getNotification());
        }

        return rollups.stream();
    }
}
