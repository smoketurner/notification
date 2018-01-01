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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.basho.riak.client.api.convert.ConversionException;
import com.basho.riak.client.api.convert.Converter;
import com.basho.riak.client.core.util.BinaryValue;
import com.google.protobuf.InvalidProtocolBufferException;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.application.protos.NotificationProtos.NotificationListPB;
import com.smoketurner.notification.application.protos.NotificationProtos.NotificationPB;
import io.dropwizard.jersey.protobuf.ProtocolBufferMediaType;

public class NotificationListConverter
        extends Converter<NotificationListObject> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(NotificationListConverter.class);

    public NotificationListConverter() {
        super(NotificationListObject.class);
    }

    @Override
    public NotificationListObject toDomain(@Nonnull final BinaryValue value,
            @Nonnull final String contentType) {
        if (!ProtocolBufferMediaType.APPLICATION_PROTOBUF.equals(contentType)) {
            LOGGER.error("Invalid Content-Type: {}", contentType);
            throw new ConversionException(
                    "Invalid Content-Type: " + contentType);
        }

        final NotificationListPB list;
        try {
            list = NotificationListPB.parseFrom(value.unsafeGetValue());
        } catch (InvalidProtocolBufferException e) {
            LOGGER.error("Unable to convert value from Riak", e);
            throw new ConversionException(e);
        }

        final NotificationListObject obj = new NotificationListObject();
        list.getNotificationList().stream()
                .map(NotificationListConverter::convert)
                .forEach(obj::addNotification);
        obj.deleteNotifications(list.getDeletedIdList());
        return obj;
    }

    @Override
    public ContentAndType fromDomain(
            @Nonnull final NotificationListObject domainObject) {
        final NotificationListPB.Builder builder = NotificationListPB
                .newBuilder().addAllDeletedId(domainObject.getDeletedIds());

        domainObject.getNotifications().stream()
                .map(NotificationListConverter::convert)
                .forEach(builder::addNotification);

        final NotificationListPB list = builder.build();

        return new ContentAndType(BinaryValue.unsafeCreate(list.toByteArray()),
                ProtocolBufferMediaType.APPLICATION_PROTOBUF);
    }

    private static Notification convert(
            @Nonnull final NotificationPB notification) {
        return Notification
                .builder(notification.getCategory(), notification.getMessage())
                .withId(notification.getId())
                .withCreatedAt(ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(notification.getCreatedAt()),
                        ZoneOffset.UTC))
                .withProperties(notification.getPropertyMap()).build();
    }

    private static NotificationPB convert(
            @Nonnull final Notification notification) {
        return NotificationPB.newBuilder().setId(notification.getId().get())
                .setCategory(notification.getCategory())
                .setMessage(notification.getMessage())
                .setCreatedAt(
                        notification.getCreatedAt().toInstant().toEpochMilli())
                .putAllProperty(notification.getProperties()).build();
    }
}
