package com.smoketurner.notification.application.riak;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.basho.riak.client.api.commands.kv.UpdateValue;

public class NotificationListDeletion extends
        UpdateValue.Update<NotificationListObject> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(NotificationListDeletion.class);
    private final Collection<Long> ids;

    /**
     * Constructor
     *
     * @param ids
     *            Notification IDs to delete
     */
    public NotificationListDeletion(@Nonnull final Collection<Long> ids) {
        this.ids = checkNotNull(ids);
    }

    @Override
    public NotificationListObject apply(NotificationListObject original) {
        if (original == null) {
            LOGGER.debug("original is null, creating new object");
            original = new NotificationListObject();
        }
        original.deleteNotifications(ids);
        return original;
    }
}
