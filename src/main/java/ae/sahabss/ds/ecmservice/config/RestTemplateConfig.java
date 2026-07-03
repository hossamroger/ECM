package ae.sahabss.ds.ecmservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Value("${conntimeout}")
    private Duration connTimeout;
    @Value("${readtimeout}")
    private Duration readTimeout;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder()
                .setConnectTimeout(connTimeout)
                .setReadTimeout(readTimeout)
                .build();
    }

}
