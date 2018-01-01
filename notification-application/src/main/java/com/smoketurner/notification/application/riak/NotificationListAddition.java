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

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.basho.riak.client.api.commands.kv.UpdateValue;
import com.smoketurner.notification.api.Notification;

public class NotificationListAddition
        extends UpdateValue.Update<NotificationListObject> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(NotificationListAddition.class);
    private final Notification notification;

    /**
     * Constructor
     *
     * @param notification
     *            Notification to add
     */
    public NotificationListAddition(@Nonnull final Notification notification) {
        this.notification = Objects.requireNonNull(notification,
                "notification == null");
    }

    @Override
    public NotificationListObject apply(
            @Nullable NotificationListObject original) {
        if (original == null) {
            LOGGER.debug("original is null, creating new notification list");
            original = new NotificationListObject();
        }
        original.addNotification(notification);
        return original;
    }
}
