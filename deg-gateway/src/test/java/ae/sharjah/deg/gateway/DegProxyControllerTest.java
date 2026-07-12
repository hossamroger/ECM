package ae.sharjah.deg.gateway;

import ae.sharjah.deg.gateway.service.DegProxyService;
import ae.sharjah.deg.gateway.web.DegProxyController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that the single dynamic controller maps every {@code /deg/**} URL
 * and extracts the correct sub-path, using a recording stub instead of Mockito.
 */
class DegProxyControllerTest {

    private final AtomicReference<String> capturedSubPath = new AtomicReference<>();
    private final AtomicReference<HttpMethod> capturedMethod = new AtomicReference<>();
    private final AtomicReference<String> capturedQuery = new AtomicReference<>();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DegProxyService recordingService = new DegProxyService(null, null, null, null, null) {
            @Override
            public ResponseEntity<byte[]> forward(String subPath, HttpMethod method, String queryString,
                                                  HttpHeaders headers, byte[] body) {
                capturedSubPath.set(subPath);
                capturedMethod.set(method);
                capturedQuery.set(queryString);
                return ResponseEntity.ok(("handled:" + subPath).getBytes());
            }
        };
        mockMvc = MockMvcBuilders.standaloneSetup(new DegProxyController(recordingService)).build();
    }

    @Test
    void mapsListEndpointAndExtractsSubPath() throws Exception {
        mockMvc.perform(get("/deg/state-list"))
                .andExpect(status().isOk())
                .andExpect(content().string("handled:/state-list"));

        assertEquals("/state-list", capturedSubPath.get());
        assertEquals(HttpMethod.GET, capturedMethod.get());
    }

    @Test
    void mapsNestedPathEndpoint() throws Exception {
        mockMvc.perform(get("/deg/city-list/7"))
                .andExpect(status().isOk());

        assertEquals("/city-list/7", capturedSubPath.get());
    }

    @Test
    void mapsAidRequestWithQueryString() throws Exception {
        // A real servlet container exposes the query via getQueryString(); MockMvc's
        // .param() does not, so set it explicitly to reflect runtime behaviour.
        mockMvc.perform(get("/deg/aid-request").with(req -> {
                    req.setQueryString("requestNo=555");
                    return req;
                }))
                .andExpect(status().isOk());

        assertEquals("/aid-request", capturedSubPath.get());
        assertEquals("requestNo=555", capturedQuery.get());
    }

    @Test
    void mapsPostUpload() throws Exception {
        mockMvc.perform(post("/deg/upload").content("payload".getBytes()))
                .andExpect(status().isOk());

        assertEquals("/upload", capturedSubPath.get());
        assertEquals(HttpMethod.POST, capturedMethod.get());
    }
}
