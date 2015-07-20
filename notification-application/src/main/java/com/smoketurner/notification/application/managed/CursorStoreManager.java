package com.smoketurner.notification.application.managed;

import io.dropwizard.lifecycle.Managed;
import javax.annotation.Nonnull;
import com.google.common.base.Preconditions;
import com.smoketurner.notification.application.store.CursorStore;

public class CursorStoreManager implements Managed {

    private final CursorStore store;

    /**
     * Constructor
     *
     * @param store
     *            Cursor store to manage
     */
    public CursorStoreManager(@Nonnull final CursorStore store) {
        this.store = Preconditions.checkNotNull(store);
    }

    @Override
    public void start() throws Exception {
        store.initialize();
    }

    @Override
    public void stop() throws Exception {
        // nothing to stop
    }
}
