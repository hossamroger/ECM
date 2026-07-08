package ae.sahabss.ds.ecmservice.ucm;

import ae.sahabss.ds.ecmservice.exceptions.TechnicalException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public interface IContent {
    public void login(String username, String password) throws Exception;

    public String upload(String contentId, String contentType, String filename, InputStream inputStream, Map<String, Object> customAttributes) throws TechnicalException, IOException, Exception;

    /**
     * Streaming variant: the caller supplies the content length explicitly
     * (InputStream.available() is only correct for in-memory streams).
     */
    public String upload(String contentId, String contentType, String filename, InputStream inputStream, long contentLength, Map<String, Object> customAttributes) throws TechnicalException, IOException, Exception;

    public InputStream download(String contentId);

    UCMDocument getDocumentInfo(String contentId);
}
