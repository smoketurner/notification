package com.smoketurner.notification.application.managed;

import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.junit.Test;
import com.smoketurner.notification.application.store.CursorStore;

public class CursorStoreManagerTest {

    private final CursorStore store = mock(CursorStore.class);

    @Test
    public void testNullManager() throws Exception {
        try {
            new CursorStoreManager(null);
            failBecauseExceptionWasNotThrown(NullPointerException.class);
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void testStart() throws Exception {
        final CursorStoreManager manager = new CursorStoreManager(store);
        manager.start();
        verify(store).initialize();
    }
}
