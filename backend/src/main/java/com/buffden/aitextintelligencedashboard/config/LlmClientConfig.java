package com.buffden.aitextintelligencedashboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class LlmClientConfig {

    @Bean
    RestClient.Builder restClientBuilder(LlmProperties llmProperties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(llmProperties.getTimeout().getConnectMs());
        factory.setReadTimeout(llmProperties.getTimeout().getReadMs());
        return RestClient.builder().requestFactory(factory);
    }
}
