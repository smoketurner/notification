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
package com.smoketurner.notification.application.riak;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.basho.riak.client.api.cap.ConflictResolver;
import com.basho.riak.client.api.cap.UnresolvedConflictException;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.smoketurner.notification.api.Notification;

public class NotificationListResolver implements
        ConflictResolver<NotificationListObject> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(NotificationListResolver.class);

    @Override
    public NotificationListObject resolve(
            final List<NotificationListObject> siblings)
            throws UnresolvedConflictException {
        LOGGER.debug("Found {} siblings", siblings.size());
        if (siblings.size() > 1) {
            final Iterator<NotificationListObject> iterator = siblings
                    .iterator();
            final NotificationListObject resolved = new NotificationListObject(
                    iterator.next());

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
                resolved.setNotifications(removeNotifications(
                        resolved.getNotifications(), deletedIds));
                resolved.clearDeletedIds();
            }

            return resolved;
        } else if (siblings.size() == 1) {

            final NotificationListObject resolved = siblings.iterator().next();

            // remove deleted notifications
            if (!resolved.getDeletedIds().isEmpty()) {
                LOGGER.debug("IDs to delete: {}", resolved.getDeletedIds());
                resolved.setNotifications(removeNotifications(
                        resolved.getNotifications(), resolved.getDeletedIds()));
                resolved.clearDeletedIds();
            }

            return resolved;
        } else {
            return null;
        }
    }

    /**
     * Remove the given notification IDs from the list of notifications. We have
     * to copy the iterable into a list to avoid modifying the original
     * reference within the {@link NotificationListObject}.
     * 
     * @param notifications
     *            Notifications to delete from
     * @param ids
     *            Notification IDs to delete
     * @return list of notifications with deleted notifications removed
     */
    public static List<Notification> removeNotifications(
            final Iterable<Notification> notifications,
            final Collection<Long> ids) {

        return ImmutableList.copyOf(Iterables.filter(notifications,
                new Predicate<Notification>() {
                    @Override
                    public boolean apply(final Notification notification) {
                        if (!notification.getId().isPresent()) {
                            return false;
                        }
                        if (ids.contains(notification.getId().get())) {
                            return false;
                        }
                        return true;
                    }
                }));
    }
}
