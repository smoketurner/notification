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
package com.smoketurner.notification.application.riak;

import static com.codahale.metrics.MetricRegistry.name;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.basho.riak.client.api.cap.ConflictResolver;
import com.basho.riak.client.api.cap.UnresolvedConflictException;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.smoketurner.notification.api.Notification;

public class NotificationListResolver
        implements ConflictResolver<NotificationListObject> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(NotificationListResolver.class);
    private final Histogram siblingCounts;

    /**
     * Constructor
     */
    public NotificationListResolver() {
        final MetricRegistry registry = SharedMetricRegistries
                .getOrCreate("default");
        this.siblingCounts = registry.histogram(
                name(NotificationListResolver.class, "sibling-counts"));
    }

    @Override
    @Nullable
    public NotificationListObject resolve(
            final List<NotificationListObject> siblings)
            throws UnresolvedConflictException {

        LOGGER.debug("Found {} siblings", siblings.size());
        siblingCounts.update(siblings.size());
        if (siblings.size() > 1) {

            final Iterator<NotificationListObject> iterator = siblings
                    .iterator();
            final NotificationListObject resolved = iterator.next();
            final Set<Long> deletedIds = resolved.getDeletedIds();

            // add all notifications
            while (iterator.hasNext()) {
                final NotificationListObject sibling = iterator.next();
                resolved.addNotifications(sibling.getNotifications());
                deletedIds.addAll(sibling.getDeletedIds());
            }

            // remove deleted notifications
            if (!deletedIds.isEmpty()) {
                LOGGER.debug("IDs to delete: {}", deletedIds);
                removeNotifications(resolved.getNotifications(), deletedIds);
            }

            return resolved;
        } else if (siblings.size() == 1) {

            final NotificationListObject resolved = siblings.get(0);

            // remove deleted notifications
            if (!resolved.getDeletedIds().isEmpty()) {
                LOGGER.debug("IDs to delete: {}", resolved.getDeletedIds());
                removeNotifications(resolved.getNotifications(),
                        resolved.getDeletedIds());
            }

            return resolved;
        } else {
            return null;
        }
    }

    /**
     * Remove the given notification IDs from the list of notifications.
     * 
     * @param notifications
     *            Notifications to delete from
     * @param ids
     *            Notification IDs to delete
     */
    public static void removeNotifications(
            final Collection<Notification> notifications,
            final Collection<Long> ids) {

        notifications.removeIf(notification -> {
            if (!notification.getId().isPresent()) {
                return true;
            }
            return ids.contains(notification.getId().get());
        });
        // clear out the original set of IDs to delete
        ids.clear();
    }
}
