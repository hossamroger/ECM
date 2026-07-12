package ae.sharjah.deg.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalised configuration for the DEG gateway.
 *
 * <p>All values that were hard-coded inside the OSB pipeline (backend URLs,
 * the static {@code s_token}, the validation service code) live here so they
 * can be supplied per-environment and kept out of source control.
 */
@Component
@ConfigurationProperties(prefix = "deg")
public class DegProperties {

    /** Base URL of the token service, e.g. https://stg-ds.sharjah.ae/socialdeptservices/api */
    private String tokenBaseUrl;

    /** Relative path of the getToken operation, appended to {@link #tokenBaseUrl}. */
    private String tokenPath = "/social-dept/token";

    /** Base URL of the backend DEG API, e.g. http://stg-sssd-api.shj.ae/api/deg */
    private String backendBaseUrl;

    /** Static header value the OSB pipeline sent to the token service as {@code s_token}. */
    private String sToken;

    private final Validation validation = new Validation();
    private final TokenCache tokenCache = new TokenCache();
    private final AidRequest aidRequest = new AidRequest();

    public static class Validation {
        /** When false the user-validation hop is skipped (useful before the security service is wired). */
        private boolean enabled = true;
        /** Full URL of the validateUser service. */
        private String url;
        /** Service code sent as the {@code dscode} header (OSB used SS-004). */
        private String dscode = "SS-004";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getDscode() { return dscode; }
        public void setDscode(String dscode) { this.dscode = dscode; }
    }

    public static class TokenCache {
        /** When true, access tokens are cached per dstoken for {@link #ttlSeconds}. */
        private boolean enabled = true;
        /** Cache time-to-live in seconds (OSB had result-caching enabled on the token service). */
        private long ttlSeconds = 300;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public long getTtlSeconds() { return ttlSeconds; }
        public void setTtlSeconds(long ttlSeconds) { this.ttlSeconds = ttlSeconds; }
    }

    public static class AidRequest {
        /** Sub-path (relative to /deg) that triggers the TRANSACTION_SEQ -> ENTITY_REQ_ID lookup. */
        private String path = "/aid-request";
        /** Inbound query parameter carrying the public request number. */
        private String inboundParam = "requestNo";
        /** Outbound query parameter expected by the backend. */
        private String outboundParam = "RequestNo";

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getInboundParam() { return inboundParam; }
        public void setInboundParam(String inboundParam) { this.inboundParam = inboundParam; }
        public String getOutboundParam() { return outboundParam; }
        public void setOutboundParam(String outboundParam) { this.outboundParam = outboundParam; }
    }

    public String getTokenUrl() {
        String base = tokenBaseUrl == null ? "" : tokenBaseUrl.replaceAll("/+$", "");
        String path = tokenPath == null ? "" : tokenPath;
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path;
    }

    public String getTokenBaseUrl() { return tokenBaseUrl; }
    public void setTokenBaseUrl(String tokenBaseUrl) { this.tokenBaseUrl = tokenBaseUrl; }
    public String getTokenPath() { return tokenPath; }
    public void setTokenPath(String tokenPath) { this.tokenPath = tokenPath; }
    public String getBackendBaseUrl() { return backendBaseUrl; }
    public void setBackendBaseUrl(String backendBaseUrl) { this.backendBaseUrl = backendBaseUrl; }
    public String getSToken() { return sToken; }
    public void setSToken(String sToken) { this.sToken = sToken; }
    public Validation getValidation() { return validation; }
    public TokenCache getTokenCache() { return tokenCache; }
    public AidRequest getAidRequest() { return aidRequest; }
}
