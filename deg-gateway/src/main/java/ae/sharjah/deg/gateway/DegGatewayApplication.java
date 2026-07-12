package ae.sharjah.deg.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * Entry point for the DEG gateway.
 *
 * <p>{@link DataSourceAutoConfiguration} is excluded so the application boots
 * even when no database is configured. A {@code DataSource} is only required by
 * the single {@code /deg/aid-request} endpoint and is wired conditionally
 * (see {@code DataSourceConfig}).
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class DegGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(DegGatewayApplication.class, args);
    }
}
