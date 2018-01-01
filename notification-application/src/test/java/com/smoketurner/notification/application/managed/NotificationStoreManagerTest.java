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
package com.smoketurner.notification.application.managed;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.junit.Test;
import com.smoketurner.notification.application.store.NotificationStore;

public class NotificationStoreManagerTest {

    private final NotificationStore store = mock(NotificationStore.class);

    @Test
    public void testStart() throws Exception {
        final NotificationStoreManager manager = new NotificationStoreManager(
                store);
        manager.start();
        verify(store).initialize();
    }
}
