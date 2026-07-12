package ae.sharjah.deg.gateway.config;

import ae.sharjah.deg.gateway.service.EntityTrnIdLookup;
import ae.sharjah.deg.gateway.service.JdbcEntityTrnIdLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.Optional;

/**
 * Wires the database only for the aid-request lookup.
 *
 * <p>If {@code deg.datasource.jndi-name} is set to a non-blank value, a JNDI
 * {@code DataSource} (e.g. {@code jdbc/DigitalSharjah}) backs the lookup.
 * Otherwise a no-op lookup is used so the application still boots and every
 * other {@code /deg} endpoint keeps working.
 */
@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    public EntityTrnIdLookup entityTrnIdLookup(
            @Value("${deg.datasource.jndi-name:}") String jndiName) {

        if (!StringUtils.hasText(jndiName)) {
            log.warn("deg.datasource.jndi-name is not set; /deg/aid-request lookups will "
                    + "resolve to empty until a datasource is configured.");
            return dsTrnId -> Optional.empty();
        }

        log.info("Configuring DEG datasource from JNDI name '{}'", jndiName);
        DataSource dataSource = new JndiDataSourceLookup().getDataSource(jndiName);
        return new JdbcEntityTrnIdLookup(new JdbcTemplate(dataSource));
    }
}
