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
package com.smoketurner.notification.application.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import org.junit.Ignore;
import org.junit.Test;
import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.buckets.StoreBucketProperties;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.UpdateValue;

public class CursorStoreTest {

    private static final String TEST_USER = "test";
    private static final String CURSOR_NAME = "notifications";
    private final RiakClient client = mock(RiakClient.class);
    private final CursorStore store = new CursorStore(client);

    @Test
    public void testInitialize() throws Exception {
        store.initialize();
        verify(client).execute(any(StoreBucketProperties.class));
    }

    @Test
    @Ignore
    public void testFetch() throws Exception {
        final FetchValue.Response response = mock(FetchValue.Response.class);

        final Optional<Long> expected = Optional.of(1L);

        when(client.execute(any(FetchValue.class))).thenReturn(response);

        final Optional<Long> actual = store.fetch(TEST_USER, CURSOR_NAME);
        verify(client).execute(any(FetchValue.class));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testFetchEmptyUsername() throws Exception {
        try {
            store.fetch("", CURSOR_NAME);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
        }
        verify(client, never()).execute(any(FetchValue.class));
    }

    @Test
    public void testFetchEmptyCursor() throws Exception {
        try {
            store.fetch("test", "");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
        }
        verify(client, never()).execute(any(FetchValue.class));
    }

    @Test
    public void testStore() throws Exception {
        store.store(TEST_USER, CURSOR_NAME, 1L);
        verify(client).executeAsync(any(UpdateValue.class));
    }

    @Test
    public void testStoreEmptyUsername() throws Exception {
        try {
            store.store("", CURSOR_NAME, 1L);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
        }
        verify(client, never()).execute(any(UpdateValue.class));
    }

    @Test
    public void testStoreEmptyCursorName() throws Exception {
        try {
            store.store(TEST_USER, "", 1L);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
        }
        verify(client, never()).execute(any(UpdateValue.class));
    }

    @Test
    public void testDelete() throws Exception {
        store.delete(TEST_USER, CURSOR_NAME);
        verify(client).executeAsync(any(DeleteValue.class));
    }

    @Test
    public void testDeleteEmptyUsername() throws Exception {
        try {
            store.delete("", CURSOR_NAME);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
        }
        verify(client, never()).execute(any(DeleteValue.class));
    }

    @Test
    public void testDeleteEmptyCursorName() throws Exception {
        try {
            store.delete(TEST_USER, "");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
        }
        verify(client, never()).execute(any(DeleteValue.class));
    }

    @Test
    public void testGetCursorKey() {
        assertThat(store.getCursorKey(TEST_USER, CURSOR_NAME))
                .isEqualTo("test-notifications");
    }
}
