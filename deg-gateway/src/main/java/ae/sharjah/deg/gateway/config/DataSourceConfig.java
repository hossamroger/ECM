package ae.sharjah.deg.gateway.config;

import ae.sharjah.deg.gateway.service.EntityTrnIdLookup;
import ae.sharjah.deg.gateway.service.JdbcEntityTrnIdLookup;
import com.zaxxer.hikari.HikariDataSource;
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
 * Wires the database used only by {@code /deg/aid-request}, in one of three modes:
 *
 * <ol>
 *   <li><b>JNDI</b> (app-server deploy): set {@code deg.datasource.jndi-name},
 *       e.g. {@code jdbc/DigitalSharjah}. Only works inside WebLogic/Tomcat.</li>
 *   <li><b>Direct JDBC</b> (local / standalone): leave the JNDI name blank and set
 *       {@code deg.datasource.url} (+ username/password). No JNDI context required.</li>
 *   <li><b>None</b>: neither is set — the app still boots and every endpoint except
 *       the aid-request DB lookup works (the lookup resolves to empty).</li>
 * </ol>
 *
 * <p>The datasource is created lazily, so an unreachable database never prevents
 * startup — only an actual aid-request call would surface the connection error.
 */
@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${deg.datasource.jndi-name:}")
    private String jndiName;

    @Value("${deg.datasource.url:}")
    private String url;

    @Value("${deg.datasource.username:}")
    private String username;

    @Value("${deg.datasource.password:}")
    private String password;

    @Value("${deg.datasource.driver-class-name:}")
    private String driverClassName;

    @Bean
    public EntityTrnIdLookup entityTrnIdLookup() {
        DataSource dataSource = resolveDataSource();
        if (dataSource == null) {
            log.warn("No datasource configured (deg.datasource.jndi-name and deg.datasource.url are "
                    + "both blank); /deg/aid-request lookups will resolve to empty.");
            return dsTrnId -> Optional.empty();
        }
        return new JdbcEntityTrnIdLookup(new JdbcTemplate(dataSource));
    }

    private DataSource resolveDataSource() {
        if (StringUtils.hasText(jndiName)) {
            log.info("Configuring DEG datasource from JNDI name '{}'", jndiName);
            return new JndiDataSourceLookup().getDataSource(jndiName);
        }
        if (StringUtils.hasText(url)) {
            log.info("Configuring DEG datasource from direct JDBC url '{}'", url);
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(url);
            if (StringUtils.hasText(username)) {
                ds.setUsername(username);
            }
            if (StringUtils.hasText(password)) {
                ds.setPassword(password);
            }
            if (StringUtils.hasText(driverClassName)) {
                ds.setDriverClassName(driverClassName);
            }
            ds.setMaximumPoolSize(5);
            // Lazy: the pool initialises on first use, so a down DB won't block startup.
            return ds;
        }
        return null;
    }
}
