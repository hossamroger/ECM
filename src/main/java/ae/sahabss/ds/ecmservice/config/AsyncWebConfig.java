package ae.sahabss.ds.ecmservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Bounded executor for StreamingResponseBody (used by /v2/downloadDocsByIds).
 * Without this, Spring falls back to SimpleAsyncTaskExecutor which spawns an
 * unbounded new thread per streaming request.
 */
@Configuration
public class AsyncWebConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(streamingTaskExecutor());
        // large bundles over slow links can legitimately take a while
        configurer.setDefaultTimeout(300_000);
    }

    @Bean
    public ThreadPoolTaskExecutor streamingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ecm-stream-");
        executor.initialize();
        return executor;
    }
}
