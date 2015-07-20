package com.smoketurner.notification.application.riak;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.basho.riak.client.api.cap.ConflictResolver;
import com.basho.riak.client.api.cap.UnresolvedConflictException;
import com.google.common.collect.ImmutableSortedSet;

public class CursorResolver implements ConflictResolver<CursorObject> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(CursorResolver.class);

    @Override
    public CursorObject resolve(final List<CursorObject> siblings)
            throws UnresolvedConflictException {
        LOGGER.debug("Found {} siblings", siblings.size());
        if (siblings.size() > 1) {
            final ImmutableSortedSet<CursorObject> cursors = ImmutableSortedSet
                    .copyOf(siblings);
            return cursors.first();
        } else if (siblings.size() == 1) {
            return siblings.iterator().next();
        } else {
            return null;
        }
    }
}