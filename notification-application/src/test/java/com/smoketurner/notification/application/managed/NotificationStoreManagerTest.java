package com.smoketurner.notification.application.managed;

import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.junit.Test;
import com.smoketurner.notification.application.store.NotificationStore;

public class NotificationStoreManagerTest {

    private final NotificationStore store = mock(NotificationStore.class);

    @Test
    public void testNullManager() throws Exception {
        try {
            new NotificationStoreManager(null);
            failBecauseExceptionWasNotThrown(NullPointerException.class);
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void testStart() throws Exception {
        final NotificationStoreManager manager = new NotificationStoreManager(
                store);
        manager.start();
        verify(store).initialize();
    }
}
