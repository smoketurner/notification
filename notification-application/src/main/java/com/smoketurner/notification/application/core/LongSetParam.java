package com.smoketurner.notification.application.core;

import io.dropwizard.jersey.params.AbstractParam;
import java.util.Set;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

public class LongSetParam extends AbstractParam<Set<Long>> {

    public LongSetParam(String input) {
        super(input);
    }

    @Override
    protected Set<Long> parse(final String input) throws Exception {
        if (Strings.isNullOrEmpty(input)) {
            return ImmutableSet.of();
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
