package ae.sharjah.deg.gateway.web;

import ae.sharjah.deg.gateway.service.DegProxyService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * The single dynamic controller that handles <em>every</em> {@code /deg/*}
 * endpoint. Instead of one handler per operation (as the OSB pipeline had 22
 * route-nodes), the sub-path is extracted at runtime and forwarded generically
 * through {@link DegProxyService}.
 */
@RestController
public class DegProxyController {

    private final DegProxyService proxyService;

    public DegProxyController(DegProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @RequestMapping("/deg/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request,
                                        @RequestHeader HttpHeaders headers,
                                        @RequestBody(required = false) byte[] body) {

        String subPath = extractSubPath(request);
        HttpMethod method = HttpMethod.resolve(request.getMethod());
        String queryString = request.getQueryString();

        return proxyService.forward(subPath, method, queryString, headers, body);
    }

    /**
     * Returns the portion of the path after {@code /deg}, always starting with
     * a '/'. For {@code /deg/state-list} this is {@code /state-list};
     * for {@code /deg} it is {@code /}.
     */
    private String extractSubPath(HttpServletRequest request) {
        String fullPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        if (fullPath == null) {
            fullPath = request.getRequestURI();
        }
        int idx = fullPath.indexOf("/deg");
        String subPath = idx >= 0 ? fullPath.substring(idx + "/deg".length()) : fullPath;
        if (!StringUtils.hasText(subPath)) {
            return "/";
        }
        return subPath.startsWith("/") ? subPath : "/" + subPath;
    }
}
