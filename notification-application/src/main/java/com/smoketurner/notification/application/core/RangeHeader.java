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
package com.smoketurner.notification.application.core;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Longs;

@Immutable
public final class RangeHeader {

    private static final Pattern ID_PATTERN = Pattern.compile(
            "(?<field>\\w+) (?<fromInclusive>\\]?)(?<fromId>\\d*)\\.\\.(?<toId>\\d*)(?<toInclusive>\\[?)");
    private static final Pattern OPTIONS_PATTERN = Pattern
            .compile("max=(?<max>\\d+)");
    private final String field;
    private final Long fromId;
    private final Boolean fromInclusive;
    private final Long toId;
    private final Boolean toInclusive;
    private final Integer max;

    /**
     * Constructor
     *
     * @param field
     * @param fromId
     * @param fromInclusive
     * @param toId
     * @param toInclusive
     * @param max
     */
    public RangeHeader(final String field, final Long fromId,
            final Boolean fromInclusive, final Long toId,
            final Boolean toInclusive, final Integer max) {
        this.field = field;
        this.fromId = fromId;
        this.fromInclusive = fromInclusive;
        this.toId = toId;
        this.toInclusive = toInclusive;
        this.max = max;
    }

    /**
     * Parse a range header
     *
     * @param header
     *            Range header to parse
     * @return parsed range header
     */
    public static RangeHeader parse(@Nullable final String header) {
        String field = null;
        Long fromId = null;
        Boolean fromInclusive = null;
        Long toId = null;
        Boolean toInclusive = null;
        Integer max = null;

        if (header == null) {
            return new RangeHeader(field, fromId, fromInclusive, toId,
                    toInclusive, max);
        }

        final List<String> parts = ImmutableList
                .copyOf(Splitter.on(';').trimResults().split(header));

        final int count = parts.size();
        if (count > 0) {
            final Matcher first = ID_PATTERN.matcher(parts.get(0));
            if (first.matches()) {
                field = first.group("field");
                fromId = Longs.tryParse(first.group("fromId"));
                if (fromId != null) {
                    fromInclusive = Strings
                            .isNullOrEmpty(first.group("fromInclusive"));
                }
                toId = Longs.tryParse(first.group("toId"));
                if (toId != null) {
                    toInclusive = Strings
                            .isNullOrEmpty(first.group("toInclusive"));
                }
            }
        }
        if (count > 1) {
            final Matcher second = OPTIONS_PATTERN.matcher(parts.get(1));
            if (second.matches()) {
                try {
                    max = Integer.parseInt(second.group("max"));
                } catch (NumberFormatException ignore) {
                }
            }
        }

        return new RangeHeader(field, fromId, fromInclusive, toId, toInclusive,
                max);
    }

    public Optional<String> getField() {
        return Optional.fromNullable(field);
    }

    public Optional<Long> getFromId() {
        return Optional.fromNullable(fromId);
    }

    public Optional<Boolean> getFromInclusive() {
        return Optional.fromNullable(fromInclusive);
    }

    public Optional<Long> getToId() {
        return Optional.fromNullable(toId);
    }

    public Optional<Boolean> getToInclusive() {
        return Optional.fromNullable(toInclusive);
    }

    public Optional<Integer> getMax() {
        return Optional.fromNullable(max);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        final RangeHeader other = (RangeHeader) obj;
        return Objects.equals(field, other.field)
                && Objects.equals(fromId, other.fromId)
                && Objects.equals(fromInclusive, other.fromInclusive)
                && Objects.equals(toId, other.toId)
                && Objects.equals(toInclusive, other.toInclusive)
                && Objects.equals(max, other.max);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, fromId, fromInclusive, toId, toInclusive,
                max);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("field", field)
                .add("fromId", fromId).add("fromInclusive", fromInclusive)
                .add("toId", toId).add("toInclusive", toInclusive)
                .add("max", max).toString();
    }
}
