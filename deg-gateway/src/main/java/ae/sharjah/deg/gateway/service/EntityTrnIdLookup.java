package ae.sharjah.deg.gateway.service;

import java.util.Optional;

/**
 * Resolves the internal {@code ENTITY_REQ_ID} for a public {@code requestNo}
 * (which maps to {@code TRANSACTION_SEQ}), mirroring the OSB DB adapter
 * {@code getEntityTrnIdByDsTrnId}:
 *
 * <pre>SELECT ENTITY_REQ_ID FROM BPM_PROCESS_TRANSACTIONS WHERE TRANSACTION_SEQ = :dsTrnId</pre>
 */
public interface EntityTrnIdLookup {

    /**
     * @param dsTrnId the public request number (TRANSACTION_SEQ)
     * @return the matching ENTITY_REQ_ID, or empty if none / not available
     */
    Optional<String> findEntityReqIdByDsTrnId(String dsTrnId);
}
