package com.smoketurner.notification.application.riak;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.basho.riak.client.api.cap.ConflictResolver;
import com.basho.riak.client.api.cap.UnresolvedConflictException;

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
                resolved.addNotifications(sibling.getNotificationList());
                deletedIds.addAll(sibling.getDeletedIds());
            }

            // remove deleted notifications
            LOGGER.debug("IDs to delete: {}", deletedIds);
            resolved.deleteNotifications(deletedIds);
            resolved.clearDeletedIds();

            return resolved;
        } else if (siblings.size() == 1) {

            final NotificationListObject resolved = siblings.iterator().next();

            LOGGER.debug("IDs to delete: {}", resolved.getDeletedIds());

            resolved.deleteNotifications(resolved.getDeletedIds());
            resolved.clearDeletedIds();

            return resolved;
        } else {
            return null;
        }
    }
}
