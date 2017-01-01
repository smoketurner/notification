/**
 * Copyright 2017 Smoke Turner, LLC.
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
package com.smoketurner.notification.application.core;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ge.snowizard.core.IdWorker;
import com.ge.snowizard.exceptions.InvalidSystemClock;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;

public class IdGenerator {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(IdGenerator.class);
    private final AtomicLong nextId = new AtomicLong(0L);
    private final IdWorker snowizard;
    private final boolean enabled;

    /**
     * Constructor
     *
     * @param snowizard
     *            Snowizard instance
     * @param enabled
     *            whether snowizard is enabled or not
     */
    public IdGenerator(@Nonnull final IdWorker snowizard,
            final boolean enabled) {
        this.snowizard = Objects.requireNonNull(snowizard);
        this.enabled = enabled;
    }

    /**
     * Generate a new notification ID
     *
     * @return the new notification ID
     * @throws NotificationStoreException
     *             if unable to generate an ID
     */
    public long nextId() throws NotificationStoreException {
        final long id;
        if (enabled) {
            try {
                id = snowizard.nextId();
            } catch (InvalidSystemClock e) {
                LOGGER.error("Clock is moving backward to generate IDs", e);
                throw new NotificationStoreException(e);
            }
        } else {
            id = nextId.getAndIncrement();
        }
        return id;
    }
}
