package com.holdhive.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

    @Test
    void exposesHoldHiveApiMetadata() {
        var openApi = new OpenApiConfig().holdHiveOpenApi();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("HoldHive API");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("0.1.0");
        assertThat(openApi.getServers())
            .singleElement()
            .satisfies(server -> assertThat(server.getUrl()).isEqualTo("/"));
    }
}
