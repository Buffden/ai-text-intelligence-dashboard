package com.buffden.aitextintelligencedashboard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class LlmClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder(
            @Value("${app.llm.timeout.connect-ms}") int connectMs,
            @Value("${app.llm.timeout.read-ms}") int readMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectMs);
        factory.setReadTimeout(readMs);
        return RestClient.builder().requestFactory(factory);
    }
}
