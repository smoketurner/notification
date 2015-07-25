package com.smoketurner.notification.application.core;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.concurrent.Immutable;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Longs;

@Immutable
public final class RangeHeader {

    private static final Pattern ID_PATTERN = Pattern
            .compile("(?<field>\\w+) (\\])?(?<id>\\d+)(\\.\\.)?");
    private static final Pattern OPTIONS_PATTERN = Pattern
            .compile("max=(?<max>\\d+)");
    private final String field;
    private final Long id;
    private final Integer max;

    /**
     * Constructor
     *
     * @param field
     * @param id
     * @param max
     */
    public RangeHeader(final String field, final Long id, final Integer max) {
        this.field = field;
        this.id = id;
        this.max = max;
    }

    public static RangeHeader parse(final String header) {
        final List<String> parts = ImmutableList.copyOf(Splitter.on(';')
                .trimResults().split(header));

        String field = null;
        Long id = null;
        Integer max = null;

        final int count = parts.size();
        if (count > 0) {
            final Matcher first = ID_PATTERN.matcher(parts.get(0));
            if (first.matches()) {
                field = first.group("field");
                id = Longs.tryParse(first.group("id"));
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

        return new RangeHeader(field, id, max);
    }

    public Optional<String> getField() {
        return Optional.fromNullable(field);
    }

    public Optional<Long> getId() {
        return Optional.fromNullable(id);
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
                && Objects.equals(id, other.id)
                && Objects.equals(max, other.max);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, id, max);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("field", field)
                .add("id", id).add("max", max).toString();
    }
}
