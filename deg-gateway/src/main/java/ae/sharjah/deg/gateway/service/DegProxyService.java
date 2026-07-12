package ae.sharjah.deg.gateway.service;

import ae.sharjah.deg.gateway.config.DegProperties;
import ae.sharjah.deg.gateway.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Orchestrates the OSB {@code ShjSocialDeptServices} flow for every
 * {@code /deg/*} sub-path:
 *
 * <ol>
 *   <li>validate the caller (401 short-circuit)</li>
 *   <li>obtain a backend access token</li>
 *   <li>[aid-request only] translate requestNo -&gt; ENTITY_REQ_ID via DB</li>
 *   <li>call the backend with {@code Authorization: Bearer &lt;token&gt;} and return its response</li>
 * </ol>
 */
@Service
public class DegProxyService {

    private static final Logger log = LoggerFactory.getLogger(DegProxyService.class);

    /** Headers that must never be forwarded to the backend as-is. */
    private static final String[] HOP_BY_HOP = {
            HttpHeaders.HOST, HttpHeaders.CONTENT_LENGTH, HttpHeaders.CONNECTION,
            HttpHeaders.TRANSFER_ENCODING, HttpHeaders.AUTHORIZATION, "s_token"
    };

    private final RestTemplate restTemplate;
    private final DegProperties props;
    private final UserValidationService validationService;
    private final DegTokenService tokenService;
    private final EntityTrnIdLookup entityTrnIdLookup;

    public DegProxyService(RestTemplate restTemplate,
                           DegProperties props,
                           UserValidationService validationService,
                           DegTokenService tokenService,
                           EntityTrnIdLookup entityTrnIdLookup) {
        this.restTemplate = restTemplate;
        this.props = props;
        this.validationService = validationService;
        this.tokenService = tokenService;
        this.entityTrnIdLookup = entityTrnIdLookup;
    }

    /**
     * @param subPath     the path after {@code /deg}, always starting with '/', e.g. {@code /state-list}
     * @param method      the inbound HTTP method
     * @param queryString the raw inbound query string (may be null)
     * @param headers     the inbound headers
     * @param body        the inbound body (may be null)
     */
    public ResponseEntity<byte[]> forward(String subPath, HttpMethod method, String queryString,
                                          HttpHeaders headers, byte[] body) {

        // Step 1: validate user
        String dstoken = headers.getFirst("dstoken");
        if (!validationService.isAuthorized(dstoken)) {
            throw new UnauthorizedException("401 UNAUTHORIZED");
        }

        // Step 2: get access token
        String accessToken = tokenService.getAccessToken(headers);

        // Build the backend URL
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(props.getBackendBaseUrl())
                .path(subPath);

        boolean aidRequest = props.getAidRequest().getPath().equalsIgnoreCase(subPath);
        if (aidRequest) {
            // Step 2b: translate the public requestNo into the internal ENTITY_REQ_ID.
            applyAidRequestQuery(uriBuilder, queryString);
        } else if (StringUtils.hasText(queryString)) {
            uriBuilder.query(queryString);
        }

        String backendUrl = uriBuilder.build(true).toUriString();

        // Step 3: call the backend with the bearer token
        HttpHeaders outHeaders = buildBackendHeaders(headers, accessToken);
        HttpEntity<byte[]> entity = new HttpEntity<>(body, outHeaders);

        log.debug("Forwarding {} {} -> {}", method, subPath, backendUrl);
        ResponseEntity<byte[]> backendResponse =
                restTemplate.exchange(backendUrl, method, entity, byte[].class);

        return copyResponse(backendResponse);
    }

    private void applyAidRequestQuery(UriComponentsBuilder uriBuilder, String queryString) {
        DegProperties.AidRequest cfg = props.getAidRequest();
        String requestNo = extractParam(queryString, cfg.getInboundParam());

        // OSB: requestNo == "null" (literal) is treated as empty.
        if ("null".equalsIgnoreCase(requestNo)) {
            requestNo = null;
        }

        String entityReqId = null;
        if (StringUtils.hasText(requestNo)) {
            Optional<String> resolved = entityTrnIdLookup.findEntityReqIdByDsTrnId(requestNo);
            entityReqId = resolved.orElse(null);
        }

        // OSB only appended RequestNo when the resolved id was non-empty.
        if (StringUtils.hasText(entityReqId)) {
            uriBuilder.queryParam(cfg.getOutboundParam(), entityReqId);
        }
    }

    private HttpHeaders buildBackendHeaders(HttpHeaders inbound, String accessToken) {
        HttpHeaders out = new HttpHeaders();
        inbound.forEach((name, values) -> {
            if (!isHopByHop(name)) {
                out.put(name, values);
            }
        });
        out.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        return out;
    }

    private ResponseEntity<byte[]> copyResponse(ResponseEntity<byte[]> backendResponse) {
        HttpHeaders responseHeaders = new HttpHeaders();
        backendResponse.getHeaders().forEach((name, values) -> {
            if (!HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase(name)) {
                responseHeaders.put(name, values);
            }
        });
        // OSB set this on every response.
        responseHeaders.set("Access-Control-Allow-Origin", "*");
        return ResponseEntity.status(backendResponse.getStatusCode())
                .headers(responseHeaders)
                .body(backendResponse.getBody());
    }

    private static boolean isHopByHop(String header) {
        for (String h : HOP_BY_HOP) {
            if (h.equalsIgnoreCase(header)) {
                return true;
            }
        }
        return false;
    }

    /** Minimal query-string parameter extraction (raw, first occurrence). */
    private static String extractParam(String queryString, String name) {
        if (!StringUtils.hasText(queryString)) {
            return null;
        }
        for (String pair : queryString.split("&")) {
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            if (key.equals(name)) {
                String value = eq >= 0 ? pair.substring(eq + 1) : "";
                try {
                    return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8.name());
                } catch (java.io.UnsupportedEncodingException e) {
                    return value; // UTF-8 is always supported; return raw as a fallback
                }
            }
        }
        return null;
    }
}
