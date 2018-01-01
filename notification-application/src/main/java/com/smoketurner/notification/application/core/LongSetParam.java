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
package com.smoketurner.notification.application.core;

import java.util.Collections;
import java.util.Set;
import javax.annotation.Nullable;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import io.dropwizard.jersey.params.AbstractParam;

public class LongSetParam extends AbstractParam<Set<Long>> {

    public LongSetParam(String input) {
        super(input);
    }

    @Override
    protected Set<Long> parse(@Nullable final String input) throws Exception {
        if (Strings.isNullOrEmpty(input)) {
            return Collections.emptySet();
        }

        final Iterable<String> splitter = Splitter.on(',').omitEmptyStrings()
                .trimResults().split(input);

        final ImmutableSet.Builder<Long> builder = ImmutableSet.builder();
        for (String value : splitter) {
            try {
                builder.add(Long.parseLong(value));
            } catch (NumberFormatException ignore) {
                // ignore invalid numbers
            }
        }
        return builder.build();
    }
}
