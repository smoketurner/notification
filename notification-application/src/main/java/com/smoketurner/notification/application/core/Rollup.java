/**
 * Copyright 2015 Smoke Turner, LLC.
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
import java.util.TreeSet;
import javax.annotation.Nonnull;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.smoketurner.notification.api.Notification;

public class Rollup {

    private final Map<String, Rule> rules;
    private final TreeSet<Matcher> matchers = Sets.newTreeSet();

    /**
     * Constructor
     *
     * @param rules
     *            Map of rules
     */
    public Rollup(@Nonnull final Map<String, Rule> rules) {
        this.rules = Preconditions.checkNotNull(rules);
    }

    /**
     * Iterates over the notifications and uses the {@link Rule} and
     * {@link Matcher} objects to roll up the notifications based on the rules.
     * 
     * @param notifications
     *            Notifications to roll up
     * @return Rolled up notifications
     */
    public Iterable<Notification> rollup(
            @Nonnull final Iterable<Notification> notifications) {
        Preconditions.checkNotNull(notifications);

        if (rules.isEmpty()) {
            return notifications;
        }

        final TreeSet<Notification> rollups = Sets.newTreeSet();

        for (final Notification notification : notifications) {
            // If the notification category doesn't match any rule categories,
            // add the notification as-is
            // to the list of rollups.
            if (!rules.containsKey(notification.getCategory())) {
                rollups.add(notification);
                continue;
            }

            final Rule rule = rules.get(notification.getCategory());

            // If we don't have any matchers yet, add the first one
            if (matchers.isEmpty()) {
                matchers.add(new Matcher(rule, notification));
                continue;
            }

            // Loop through the existing matchers to see if this notification
            // falls into any previous
            // rollups
            boolean matched = false;
            for (final Matcher match : matchers) {
                if (match.add(notification)) {
                    matched = true;
                    break;
                }
            }

            // If the notification didn't match any existing rollups, add it as
            // a new matcher
            if (!matched) {
                matchers.add(new Matcher(rule, notification));
            }
        }

        // Pull out the rolled up notifications out of the matchers
        for (final Matcher match : matchers) {
            rollups.add(match.getNotification());
        }

        return FluentIterable.from(rollups);
    }
}
