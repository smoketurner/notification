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

public class CursorUpdate extends UpdateValue.Update<CursorObject> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(CursorUpdate.class);
    private final String key;
    private final long value;

    /**
     * Constructor
     * 
     * @param key
     * @param value
     */
    public CursorUpdate(@Nonnull final String key, final long value) {
        this.key = Objects.requireNonNull(key, "key == null");
        this.value = value;
    }

    @Override
    public CursorObject apply(@Nullable CursorObject original) {
        if (original == null) {
            LOGGER.debug("original is null, creating new cursor");
            original = new CursorObject(key, value);
        }
        original.setValue(value);
        return original;
    }
}
