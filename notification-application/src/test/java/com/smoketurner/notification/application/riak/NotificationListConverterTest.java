/**
 * Copyright 2016 Smoke Turner, LLC.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import com.basho.riak.client.api.convert.ConversionException;
import com.basho.riak.client.core.util.BinaryValue;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.application.protos.NotificationProtos.NotificationListPB;
import com.smoketurner.notification.application.protos.NotificationProtos.NotificationPB;

public class NotificationListConverterTest {

    private final NotificationListConverter converter = new NotificationListConverter();

    @Test
    public void testToDomainInvalidContentType() throws Exception {
        try {
            converter.toDomain(BinaryValue.create("test"), "text/plain");
            failBecauseExceptionWasNotThrown(ConversionException.class);
        } catch (ConversionException e) {
        }
    }

    @Test
    public void testToDomainInvalidData() throws Exception {
        try {
            converter.toDomain(BinaryValue.create("test"),
                    "application/x-protobuf");
            failBecauseExceptionWasNotThrown(ConversionException.class);
        } catch (ConversionException e) {
        }
    }

    @Test
    public void testToDomain() throws Exception {
        final DateTime now = new DateTime("2015-08-14T17:52:43Z",
                DateTimeZone.UTC);

        final Notification n1 = Notification.builder().withId(1L)
                .withCategory("test-category").withMessage("this is a test")
                .withCreatedAt(now).build();
        final NotificationListObject expected = new NotificationListObject();
        expected.addNotification(n1);

        final NotificationPB pb = NotificationPB.newBuilder().setId(1L)
                .setCategory("test-category").setMessage("this is a test")
                .setCreatedAt(now.getMillis()).build();

        final NotificationListPB list = NotificationListPB.newBuilder()
                .addNotification(pb).build();

        final NotificationListObject actual = converter.toDomain(
                BinaryValue.create(list.toByteArray()),
                "application/x-protobuf");
        assertThat(actual).isEqualTo(expected);
    }
}
