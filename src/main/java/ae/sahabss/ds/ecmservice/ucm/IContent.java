package ae.sahabss.ds.ecmservice.ucm;

import ae.sahabss.ds.ecmservice.exceptions.TechnicalException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public interface IContent {
    public void login(String username, String password) throws Exception;

    public String upload(String contentId, String contentType, String filename, InputStream inputStream, Map<String, Object> customAttributes) throws TechnicalException, IOException, Exception;

    public InputStream download(String contentId);

    UCMDocument getDocumentInfo(String contentId);
}
