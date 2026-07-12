package ae.sharjah.deg.gateway.service;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

/**
 * JDBC-backed implementation of {@link EntityTrnIdLookup}, used when a
 * {@code DataSource} is configured (production / staging).
 */
public class JdbcEntityTrnIdLookup implements EntityTrnIdLookup {

    private static final String SQL =
            "SELECT ENTITY_REQ_ID FROM BPM_PROCESS_TRANSACTIONS WHERE TRANSACTION_SEQ = ?";

    private final JdbcTemplate jdbcTemplate;

    public JdbcEntityTrnIdLookup(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<String> findEntityReqIdByDsTrnId(String dsTrnId) {
        if (dsTrnId == null || dsTrnId.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            String entityReqId = jdbcTemplate.queryForObject(SQL, String.class, dsTrnId.trim());
            return Optional.ofNullable(entityReqId);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }
}
