package com.smoketurner.notification.application.riak;

import static com.google.common.base.Preconditions.checkNotNull;
import com.basho.riak.client.api.commands.kv.UpdateValue;
import com.smoketurner.notification.api.Notification;

public class NotificationListAddition extends
        UpdateValue.Update<NotificationListObject> {

    private final Notification notification;

    /**
     * Constructor
     *
     * @param notification
     *            Notification to add
     */
    public NotificationListAddition(final Notification notification) {
        this.notification = checkNotNull(notification);
    }

    @Override
    public NotificationListObject apply(NotificationListObject original) {
        if (original == null) {
            original = new NotificationListObject();
        }
        original.addNotification(notification);
        return original;
    }
}
