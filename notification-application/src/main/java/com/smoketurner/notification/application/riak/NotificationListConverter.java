package com.smoketurner.notification.application.riak;

import io.dropwizard.jersey.protobuf.ProtocolBufferMediaType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.basho.riak.client.api.convert.ConversionException;
import com.basho.riak.client.api.convert.Converter;
import com.basho.riak.client.core.util.BinaryValue;
import com.google.protobuf.InvalidProtocolBufferException;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.application.protos.NotificationProtos.NotificationListPB;
import com.smoketurner.notification.application.protos.NotificationProtos.NotificationPB;

public class NotificationListConverter extends
        Converter<NotificationListObject> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(NotificationListConverter.class);

    public NotificationListConverter() {
        super(NotificationListObject.class);
    }

    @Override
    public NotificationListObject toDomain(BinaryValue value, String contentType)
            throws ConversionException {

        if (!ProtocolBufferMediaType.APPLICATION_PROTOBUF.equals(contentType)) {
            LOGGER.error("Invalid Content-Type: {}", contentType);
            throw new ConversionException("Invalid Content-Type: "
                    + contentType);
        }

        final NotificationListPB list;
        try {
            list = NotificationListPB.parseFrom(value.unsafeGetValue());
        } catch (InvalidProtocolBufferException e) {
            LOGGER.error("Unable to convert value from Riak", e);
            throw new ConversionException(e);
        }

        final NotificationListObject obj = new NotificationListObject();
        for (NotificationPB notification : list.getNotificationList()) {
            obj.addNotification(convert(notification));
        }
        return obj;
    }

    @Override
    public ContentAndType fromDomain(NotificationListObject domainObject)
            throws ConversionException {

        final NotificationListPB.Builder builder = NotificationListPB
                .newBuilder();
        for (Notification notification : domainObject.getNotificationList()) {
            builder.addNotification(convert(notification));
        }
        final NotificationListPB list = builder.build();

        return new ContentAndType(BinaryValue.unsafeCreate(list.toByteArray()),
                ProtocolBufferMediaType.APPLICATION_PROTOBUF);
    }

    public static Notification convert(final NotificationPB notification) {
        return Notification
                .newBuilder()
                .withId(notification.getId())
                .withCategory(notification.getCategory())
                .withMessage(notification.getMessage())
                .withCreatedAt(
                        new DateTime(notification.getCreatedAt(),
                                DateTimeZone.UTC)).build();
    }

    public static NotificationPB convert(final Notification notification) {
        return NotificationPB.newBuilder().setId(notification.getId().get())
                .setCategory(notification.getCategory())
                .setMessage(notification.getMessage())
                .setCreatedAt(notification.getCreatedAt().get().getMillis())
                .build();
    }
}
