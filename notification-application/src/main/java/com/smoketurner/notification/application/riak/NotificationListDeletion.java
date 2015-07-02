package com.smoketurner.notification.application.riak;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Collection;
import javax.annotation.Nonnull;
import com.basho.riak.client.api.commands.kv.UpdateValue;

public class NotificationListDeletion extends
        UpdateValue.Update<NotificationListObject> {

    private final Collection<Long> ids;

    public NotificationListDeletion(@Nonnull final Collection<Long> ids) {
        this.ids = checkNotNull(ids);
    }

    @Override
    public NotificationListObject apply(NotificationListObject original) {
        if (original == null) {
            original = new NotificationListObject();
        }
        original.deleteNotifications(ids);
        return original;
    }
}
