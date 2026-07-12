package ae.sharjah.deg.gateway.service;

import ae.sharjah.deg.gateway.config.DegProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Step 2 of the OSB flow: obtain the backend access token from the token
 * service, forwarding the same headers OSB did
 * ({@code dstoken, language, uuid, devicetype} plus the static {@code s_token}).
 * The token is read from {@code data.token} in the JSON response.
 *
 * <p>Results are cached per {@code dstoken} for a short TTL, mirroring the
 * result-caching that was enabled on the OSB token business service.
 */
@Service
public class DegTokenService {

    private static final Logger log = LoggerFactory.getLogger(DegTokenService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestTemplate restTemplate;
    private final DegProperties props;
    private final ConcurrentHashMap<String, CachedToken> cache = new ConcurrentHashMap<>();

    public DegTokenService(RestTemplate restTemplate, DegProperties props) {
        this.restTemplate = restTemplate;
        this.props = props;
    }

    /**
     * @param passthroughHeaders the caller's inbound headers, used to forward
     *                           dstoken/language/uuid/devicetype to the token service
     * @return the bearer access token
     */
    public String getAccessToken(HttpHeaders passthroughHeaders) {
        String dstoken = firstHeader(passthroughHeaders, "dstoken");
        String cacheKey = dstoken == null ? "" : dstoken;

        if (props.getTokenCache().isEnabled()) {
            CachedToken cached = cache.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                return cached.token;
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        copyHeader(passthroughHeaders, headers, "dstoken");
        copyHeader(passthroughHeaders, headers, "language");
        copyHeader(passthroughHeaders, headers, "uuid");
        copyHeader(passthroughHeaders, headers, "devicetype");
        if (StringUtils.hasText(props.getSToken())) {
            headers.add("s_token", props.getSToken());
        }

        ResponseEntity<String> response = restTemplate.exchange(
                props.getTokenUrl(), HttpMethod.GET, new HttpEntity<>(null, headers), String.class);

        String token = extractToken(response.getBody());
        if (!StringUtils.hasText(token)) {
            throw new IllegalStateException("Token service did not return data.token");
        }

        if (props.getTokenCache().isEnabled()) {
            long ttlMillis = props.getTokenCache().getTtlSeconds() * 1000L;
            cache.put(cacheKey, new CachedToken(token, System.currentTimeMillis() + ttlMillis));
        }
        return token;
    }

    private String extractToken(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        try {
            JsonNode data = MAPPER.readTree(body).get("data");
            JsonNode token = data == null ? null : data.get("token");
            return token == null ? null : token.asText();
        } catch (Exception ex) {
            log.warn("Unable to parse token response: {}", ex.toString());
            return null;
        }
    }

    private static void copyHeader(HttpHeaders from, HttpHeaders to, String name) {
        String value = firstHeader(from, name);
        if (value != null) {
            to.add(name, value);
        }
    }

    private static String firstHeader(HttpHeaders headers, String name) {
        if (headers == null) {
            return null;
        }
        return headers.getFirst(name);
    }

    private static final class CachedToken {
        private final String token;
        private final long expiresAt;

        CachedToken(String token, long expiresAt) {
            this.token = token;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }
}
