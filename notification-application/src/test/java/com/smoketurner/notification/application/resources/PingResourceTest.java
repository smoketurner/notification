package com.smoketurner.notification.application.resources;

import static org.assertj.core.api.Assertions.assertThat;
import io.dropwizard.testing.junit.ResourceTestRule;
import javax.ws.rs.core.Response;
import org.junit.ClassRule;
import org.junit.Test;

public class PingResourceTest {

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new PingResource()).build();

    @Test
    public void testGetPing() throws Exception {
        final Response response = resources.client().target("/ping").request()
                .get();
        final String actual = response.readEntity(String.class);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(actual).isEqualTo("pong");
    }
}
