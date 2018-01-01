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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.primitives.Longs;

@Immutable
public final class RangeHeader {

    private static final Pattern ID_PATTERN = Pattern.compile(
            "(?<field>\\w+) (?<fromInclusive>\\]?)(?<fromId>\\d*)\\.\\.(?<toId>\\d*)(?<toInclusive>\\[?)");
    private static final Pattern OPTIONS_PATTERN = Pattern
            .compile("max=(?<max>\\d+)");

    @Nullable
    private final String field;

    @NotNull
    private final Range<Long> range;

    @Nullable
    private final Integer max;

    /**
     * Constructor
     *
     * @param builder
     */
    private RangeHeader(final Builder builder) {

        this.field = builder.field;

        if (builder.fromId != null && builder.toId != null) {
            if (builder.fromInclusive && builder.toInclusive) {
                range = Range.closed(builder.fromId, builder.toId);
            } else if (builder.fromInclusive && !builder.toInclusive) {
                range = Range.closedOpen(builder.fromId, builder.toId);
            } else if (!builder.fromInclusive && builder.toInclusive) {
                range = Range.openClosed(builder.fromId, builder.toId);
            } else {
                range = Range.open(builder.fromId, builder.toId);
            }
        } else if (builder.fromId != null && builder.toId == null) {
            if (builder.fromInclusive) {
                range = Range.atLeast(builder.fromId);
            } else {
                range = Range.greaterThan(builder.fromId);
            }
        } else if (builder.fromId == null && builder.toId != null) {
            if (builder.toInclusive) {
                range = Range.atMost(builder.toId);
            } else {
                range = Range.lessThan(builder.toId);
            }
        } else {
            range = Range.all();
        }

        this.max = builder.max;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static RangeHeader create() {
        return builder().build();
    }

    public static class Builder {

        @Nullable
        private String field;

        @Nullable
        private Long fromId;

        private boolean fromInclusive = false;

        @Nullable
        private Long toId;

        private boolean toInclusive = false;

        @Nullable
        private Integer max;

        public Builder field(@Nullable final String field) {
            this.field = field;
            return this;
        }

        public Builder fromId(@Nullable final Long id) {
            if (id == null) {
                fromInclusive = false;
            }
            this.fromId = id;
            return this;
        }

        public Builder fromInclusive(final boolean inclusive) {
            this.fromInclusive = inclusive;
            return this;
        }

        public Builder toId(@Nullable final Long id) {
            if (id == null) {
                toInclusive = false;
            }
            this.toId = id;
            return this;
        }

        public Builder toInclusive(final boolean inclusive) {
            this.toInclusive = inclusive;
            return this;
        }

        public Builder max(@Nullable final Integer max) {
            this.max = max;
            return this;
        }

        public RangeHeader build() {
            return new RangeHeader(this);
        }
    }

    /**
     * Parse a range header
     *
     * @param header
     *            Range header to parse
     * @return parsed range header
     */
    public static RangeHeader parse(@Nullable final String header) {
        if (header == null) {
            return RangeHeader.create();
        }

        final RangeHeader.Builder builder = RangeHeader.builder();

        final List<String> parts = ImmutableList
                .copyOf(Splitter.on(';').trimResults().split(header));

        final int count = parts.size();
        if (count > 0) {
            final Matcher first = ID_PATTERN.matcher(parts.get(0));
            if (first.matches()) {

                final Long fromId = Longs.tryParse(first.group("fromId"));
                final Long toId = Longs.tryParse(first.group("toId"));

                builder.field(first.group("field")).fromId(fromId).toId(toId);

                if (fromId != null) {
                    builder.fromInclusive(Strings
                            .isNullOrEmpty(first.group("fromInclusive")));
                }

                if (toId != null) {
                    builder.toInclusive(
                            Strings.isNullOrEmpty(first.group("toInclusive")));
                }
            }
        }
        if (count > 1) {
            final Matcher second = OPTIONS_PATTERN.matcher(parts.get(1));
            if (second.matches()) {
                try {
                    builder.max(Integer.parseInt(second.group("max")));
                } catch (NumberFormatException ignore) {
                    // ignore
                }
            }
        }

        return builder.build();
    }

    public Optional<String> getField() {
        return Optional.ofNullable(field);
    }

    public Optional<Long> getFromId() {
        if (!range.hasLowerBound()) {
            return Optional.empty();
        }
        return Optional.of(range.lowerEndpoint());
    }

    public Optional<Boolean> getFromInclusive() {
        if (!range.hasLowerBound()) {
            return Optional.empty();
        }
        return Optional.of(range.lowerBoundType() == BoundType.CLOSED);
    }

    public Optional<Long> getToId() {
        if (!range.hasUpperBound()) {
            return Optional.empty();
        }
        return Optional.of(range.upperEndpoint());
    }

    public Optional<Boolean> getToInclusive() {
        if (!range.hasUpperBound()) {
            return Optional.empty();
        }
        return Optional.of(range.upperBoundType() == BoundType.CLOSED);
    }

    public Optional<Integer> getMax() {
        return Optional.ofNullable(max);
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
                && Objects.equals(range, other.range)
                && Objects.equals(max, other.max);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, range, max);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("field", field)
                .add("range", range).add("max", max).toString();
    }
}
