package com.smoketurner.notification.application.riak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.basho.riak.client.api.commands.kv.UpdateValue;
import com.google.common.base.Preconditions;
import com.smoketurner.notification.api.Notification;

public class NotificationListAddition extends
        UpdateValue.Update<NotificationListObject> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(NotificationListAddition.class);
    private final Notification notification;

    /**
     * Constructor
     *
     * @param notification
     *            Notification to add
     */
    public NotificationListAddition(final Notification notification) {
        this.notification = Preconditions.checkNotNull(notification);
    }

    @Override
    public NotificationListObject apply(NotificationListObject original) {
        if (original == null) {
            LOGGER.debug("original is null, creating new object");
            original = new NotificationListObject();
        }
        original.addNotification(notification);
        return original;
    }
}
