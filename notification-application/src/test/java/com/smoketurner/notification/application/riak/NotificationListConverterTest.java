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

import static org.assertj.core.api.Assertions.assertThat;
import java.time.ZonedDateTime;
import org.junit.Test;
import com.basho.riak.client.api.convert.ConversionException;
import com.basho.riak.client.core.util.BinaryValue;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.application.protos.NotificationProtos.NotificationListPB;
import com.smoketurner.notification.application.protos.NotificationProtos.NotificationPB;

public class NotificationListConverterTest {

    private final NotificationListConverter converter = new NotificationListConverter();

    @Test(expected = ConversionException.class)
    public void testToDomainInvalidContentType() throws Exception {
        converter.toDomain(BinaryValue.create("test"), "text/plain");
    }

    @Test(expected = ConversionException.class)
    public void testToDomainInvalidData() throws Exception {
        converter.toDomain(BinaryValue.create("test"),
                "application/x-protobuf");
    }

    @Test
    public void testToDomain() throws Exception {
        final ZonedDateTime now = ZonedDateTime.parse("2015-08-14T17:52:43Z");

        final Notification n1 = Notification
                .builder("test-category", "this is a test").withId(1L)
                .withCreatedAt(now).build();
        final NotificationListObject expected = new NotificationListObject();
        expected.addNotification(n1);

        final NotificationPB pb = NotificationPB.newBuilder().setId(1L)
                .setCategory("test-category").setMessage("this is a test")
                .setCreatedAt(now.toInstant().toEpochMilli()).build();

        final NotificationListPB list = NotificationListPB.newBuilder()
                .addNotification(pb).build();

        final NotificationListObject actual = converter.toDomain(
                BinaryValue.create(list.toByteArray()),
                "application/x-protobuf");
        assertThat(actual).isEqualTo(expected);
    }
}
