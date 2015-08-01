/**
 * Copyright 2015 Smoke Turner, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.notification.application.core;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.smoketurner.notification.api.Notification;

public class Rollup {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(Rollup.class);
    private final Map<String, Rule> rules;
    private final List<Matcher> matchers = Lists.newArrayList();

    /**
     * Constructor
     *
     * @param rules
     *            Map of rules
     */
    public Rollup(@Nonnull final Map<String, Rule> rules) {
        this.rules = Preconditions.checkNotNull(rules);
    }

    public Iterable<Notification> rollup(
            @Nonnull final Iterable<Notification> notifications) {

        final TreeSet<Notification> rollups = Sets.newTreeSet();

        final Iterator<Notification> iterator = notifications.iterator();
        while (iterator.hasNext()) {
            final Notification notification = iterator.next();

            if (!rules.containsKey(notification.getCategory())) {
                LOGGER.debug("No rule found for category: {}",
                        notification.getCategory());
                rollups.add(notification);
                continue;
            }

            final Rule rule = rules.get(notification.getCategory());

            if (matchers.size() < 1) {
                matchers.add(new Matcher(rule, notification));
                continue;
            }

            boolean matched = false;
            for (final Matcher match : matchers) {
                if (match.add(notification)) {
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                matchers.add(new Matcher(rule, notification));
            }
        }

        for (final Matcher match : matchers) {
            rollups.add(match.getNotification());
        }

        return FluentIterable.from(rollups);
    }
}
