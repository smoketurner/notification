package com.smoketurner.notification.application.resources;

import static org.assertj.core.api.Assertions.assertThat;
import io.dropwizard.testing.junit.ResourceTestRule;
import javax.ws.rs.core.Response;
import org.junit.ClassRule;
import org.junit.Test;

public class VersionResourceTest {

    private static final String VERSION = "1.0.0-TEST";

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new VersionResource(VERSION)).build();

    @Test
    public void testVersion() throws Exception {
        final Response response = resources.client().target("/version")
                .request().get();
        final String actual = response.readEntity(String.class);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(actual).isEqualTo(VERSION);
    }
}
