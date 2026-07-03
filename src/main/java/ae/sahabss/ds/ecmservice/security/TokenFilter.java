package ae.sahabss.ds.ecmservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class TokenFilter extends OncePerRequestFilter {

    private final Logger logger = LoggerFactory.getLogger(TokenFilter.class);

    @Value("${ds-token-url}")
    private String intTokenUrl;

    @Value("${ds-ext-token-url}")
    private String extTokenUrl;
    @Value("${ds-nty-token-url}")
    private String entityTokenUrl;

    @Resource(name = "getUserDetails")
    private UserDetails userDetails;

    private RestTemplate restTemplate;

    @Autowired
    public TokenFilter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        System.out.println("header /n -- language " + request.getHeader("language") + "/n -- dstoken " + request.getHeader("dstoken"));

        // to get request locale from any place in the code
        userDetails.setUserCurrentLocale(request.getHeader("language") == null ? "en" : request.getHeader("language"));


        String token =
                request.getHeader("dstoken") == null ? null : request.getHeader("dstoken").trim();

        String source = request.getHeader("source") != null ? request.getHeader("source") : "";
        String tokenUrl = (source != null && (source.equalsIgnoreCase("EXT") || source.equalsIgnoreCase("NTY"))) ? entityTokenUrl : intTokenUrl;


        if (token != null) {

            HttpHeaders headers = createTokenHeaders(token);
            HttpEntity<UserDetails> httpEntity = new HttpEntity<>(null, headers);
            ResponseEntity<UserDetails> user;

            try {
                user = restTemplate.exchange(tokenUrl, HttpMethod.GET, httpEntity, UserDetails.class);
                String body = decodeJWTBody(token);
                TokenBody dsTokenBody = new ObjectMapper().readValue(body, TokenBody.class);

                if (source != "" && ("NTY".equalsIgnoreCase(source) || "EXT".equalsIgnoreCase(source)) && user.getBody().getStatus().equalsIgnoreCase("200")) {
                    userDetails.setUserId(dsTokenBody.getCustomData().getDsUserCode());
                    userDetails.setEntity(dsTokenBody.getCustomData().getEntity());
                    userDetails.setResponseCode("0");

                    chain.doFilter(request, response);
                } else if (user.getBody().getStatusCode().equals("200")) {

                    userDetails.setResponseCode("0");
                    userDetails.setUserId(dsTokenBody.getCustomData().getCode());
                    userDetails.setIdn(user.getBody().getIdn());
                    userDetails.setMobile(user.getBody().getMobile());
                    userDetails.setIdType(user.getBody().getIdType());

                } else {
                    logger.warn("Invalid token !");
                    userDetails.setResponseCode("1");
                    response.setStatus(401);
                    return;
                }
            } catch (Exception ex) {
                logger.warn(String.valueOf(ex));
            }

        } else {
            logger.warn("No token provided !");
            response.setStatus(401);
            return;
        }

        chain.doFilter(request, response);
    }

    private HttpHeaders createTokenHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("dstoken", token);
        headers.add("dscode", "NM-001");
        return headers;
    }

    public static String decodeJWTBody(String jwtToken) {
        String[] splitToken = jwtToken.split("\\.");
        String encodedBody = splitToken[1];
        Base64 base64Url = new Base64(true);
        String body = new String(base64Url.decode(encodedBody));
        return body;
    }

    //  private void raiseException(HttpServletRequest request, HttpServletResponse response)
    //      throws IOException, ServletException {
    //    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    //    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    //    byte[] body =
    //        new ObjectMapper()
    //            .writeValueAsBytes(
    //                new ApplicationException(
    //                    "Invalid Token", "Invalid Token", ApplicationError.BAD_CREDENTIALS));
    //    response.getOutputStream().write(body);
    //  }
}
