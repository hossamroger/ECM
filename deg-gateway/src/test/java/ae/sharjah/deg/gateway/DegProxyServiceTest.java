package ae.sharjah.deg.gateway;

import ae.sharjah.deg.gateway.config.DegProperties;
import ae.sharjah.deg.gateway.exception.UnauthorizedException;
import ae.sharjah.deg.gateway.service.DegProxyService;
import ae.sharjah.deg.gateway.service.DegTokenService;
import ae.sharjah.deg.gateway.service.EntityTrnIdLookup;
import ae.sharjah.deg.gateway.service.UserValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Verifies the full OSB flow (validate -> token -> [db] -> backend) end to end
 * against a stubbed HTTP layer, without booting a Spring context.
 */
class DegProxyServiceTest {

    private static final String VALIDATE_URL = "http://validate.local/validate";
    private static final String TOKEN_URL = "http://token.local/api/social-dept/token";
    private static final String BACKEND_BASE = "http://backend.local/api/deg";
    private static final String TOKEN_JSON = "{\"data\":{\"token\":\"ACC-123\"}}";

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private DegProperties props;
    private AtomicReference<String> lookupArg;
    private DegProxyService service;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        // Mirror the production RestTemplate bean: never throw on non-2xx so the
        // backend status is passed through to the caller.
        restTemplate.setErrorHandler(new org.springframework.web.client.ResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
                return false;
            }

            @Override
            public void handleError(org.springframework.http.client.ClientHttpResponse response) {
            }
        });
        server = MockRestServiceServer.createServer(restTemplate);

        props = new DegProperties();
        props.setTokenBaseUrl("http://token.local/api");
        props.setTokenPath("/social-dept/token");
        props.setBackendBaseUrl(BACKEND_BASE);
        props.setSToken("S3CR3T");
        props.getValidation().setEnabled(true);
        props.getValidation().setUrl(VALIDATE_URL);
        props.getTokenCache().setEnabled(false);

        UserValidationService validation = new UserValidationService(restTemplate, props);
        DegTokenService token = new DegTokenService(restTemplate, props);

        lookupArg = new AtomicReference<>();
        EntityTrnIdLookup lookup = dsTrnId -> {
            lookupArg.set(dsTrnId);
            return Optional.of("ENT-999");
        };

        service = new DegProxyService(restTemplate, props, validation, token, lookup);
    }

    private HttpHeaders inboundHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("dstoken", "USER-TOKEN");
        headers.add("language", "en");
        headers.add("uuid", "device-uuid");
        headers.add("devicetype", "ios");
        return headers;
    }

    @Test
    void genericEndpoint_runsValidateThenTokenThenBackend() {
        server.expect(requestTo(VALIDATE_URL)).andExpect(method(HttpMethod.POST))
                .andExpect(header("dstoken", "USER-TOKEN"))
                .andExpect(header("dscode", "SS-004"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        server.expect(requestTo(TOKEN_URL)).andExpect(method(HttpMethod.GET))
                .andExpect(header("s_token", "S3CR3T"))
                .andExpect(header("dstoken", "USER-TOKEN"))
                .andRespond(withSuccess(TOKEN_JSON, MediaType.APPLICATION_JSON));

        server.expect(requestTo(BACKEND_BASE + "/state-list")).andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer ACC-123"))
                .andRespond(withSuccess("[{\"id\":1}]", MediaType.APPLICATION_JSON));

        ResponseEntity<byte[]> response =
                service.forward("/state-list", HttpMethod.GET, null, inboundHeaders(), null);

        server.verify();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals("[{\"id\":1}]".getBytes(StandardCharsets.UTF_8), response.getBody());
        assertEquals("*", response.getHeaders().getFirst("Access-Control-Allow-Origin"));
    }

    @Test
    void unauthorized_shortCircuitsBeforeTokenAndBackend() {
        server.expect(requestTo(VALIDATE_URL)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"statusCode\":\"401\"}", MediaType.APPLICATION_JSON));

        assertThrows(UnauthorizedException.class, () ->
                service.forward("/gender-list", HttpMethod.GET, null, inboundHeaders(), null));

        // Only the validate call happened; token/backend were never invoked.
        server.verify();
    }

    @Test
    void aidRequest_translatesRequestNoToEntityReqId() {
        server.expect(requestTo(VALIDATE_URL)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString(BACKEND_BASE + "/aid-request?RequestNo=ENT-999")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer ACC-123"))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        ResponseEntity<byte[]> response =
                service.forward("/aid-request", HttpMethod.GET, "requestNo=555", inboundHeaders(), null);

        server.verify();
        assertEquals("555", lookupArg.get());
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void aidRequest_withLiteralNull_skipsLookupAndSendsNoRequestNo() {
        server.expect(requestTo(VALIDATE_URL)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_JSON, MediaType.APPLICATION_JSON));
        // No RequestNo query param should be appended.
        server.expect(requestTo(BACKEND_BASE + "/aid-request"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        service.forward("/aid-request", HttpMethod.GET, "requestNo=null", inboundHeaders(), null);

        server.verify();
        // lookup must not have been called for the literal "null"
        assertFalse("null".equals(lookupArg.get()));
    }

    @Test
    void backendErrorStatus_isPassedThrough() {
        server.expect(requestTo(VALIDATE_URL)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BACKEND_BASE + "/document"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("not found"));

        ResponseEntity<byte[]> response =
                service.forward("/document", HttpMethod.GET, null, inboundHeaders(), null);

        server.verify();
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertArrayEquals("not found".getBytes(StandardCharsets.UTF_8), response.getBody());
    }
}
