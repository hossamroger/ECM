package ae.sahabss.ds.ecmservice.controller;

import ae.sahabss.ds.ecmservice.dto.*;
import ae.sahabss.ds.ecmservice.exceptions.TechnicalException;
import ae.sahabss.ds.ecmservice.service.ECMService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class ECMServiceControllerWrapper {

    private final ECMService ecmService;

    public ECMServiceControllerWrapper(ECMService ecmService) {
        this.ecmService = ecmService;
    }

    public GenericResponse uploadDoc(UploadDocRequest request) {
        GenericResponse genericResponse = new GenericResponse();
        try {
            UploadDocResponse uploadDocResponse = ecmService.uploadDoc(request);
            genericResponse.setStatusCode(HttpStatus.OK.value());
            genericResponse.setErrorFlag("F");
            genericResponse.setData(uploadDocResponse);
        } catch (TechnicalException e) {
            e.printStackTrace();
            genericResponse.setErrorFlag("T");
            genericResponse.setStatusCode(HttpStatus.OK.value());
            genericResponse.setMsgCodeAndText(ecmService.getMessage(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            genericResponse.setErrorFlag("T");
            genericResponse.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            genericResponse.setMsg(e.getMessage());
        }

        return genericResponse;
    }

    public GenericResponse downloadDoc(DownloadDocRequest request) {
        GenericResponse genericResponse = new GenericResponse();
        try {
            DownloadDocResponse downloadDocResponse = ecmService.downloadDoc(request);
            genericResponse.setStatusCode(HttpStatus.OK.value());
            genericResponse.setErrorFlag("F");
            genericResponse.setData(downloadDocResponse);
        } catch (TechnicalException e) {
            e.printStackTrace();
            genericResponse.setErrorFlag("T");
            genericResponse.setStatusCode(HttpStatus.OK.value());
            genericResponse.setMsgCodeAndText(ecmService.getMessage(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            genericResponse.setErrorFlag("T");
            genericResponse.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            genericResponse.setMsg(e.getMessage());
        }

        return genericResponse;
    }

    public GenericResponse getAllowedUploadDocumentsByEntityId(Integer entityId) {
        GenericResponse genericResponse = new GenericResponse();
        try {
            AllowedUploadDocumentsResponse allowedUploadDocumentsResponse = ecmService.getAllowedUploadDocumentsByEntityId(entityId);
            genericResponse.setStatusCode(HttpStatus.OK.value());
            genericResponse.setErrorFlag("F");
            genericResponse.setData(allowedUploadDocumentsResponse);
        } catch (Exception e) {
            e.printStackTrace();
            genericResponse.setErrorFlag("T");
            genericResponse.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            genericResponse.setMsg(e.getMessage());
        }
        return genericResponse;
    }

    public GenericResponse downloadDocsByIds(List<String> docIdsList) {
        GenericResponse genericResponse = new GenericResponse();
        try {
            DownloadDocResponse downloadDocsResponse = ecmService.downloadDocsByIds(docIdsList);
            genericResponse.setStatusCode(HttpStatus.OK.value());
            genericResponse.setErrorFlag("F");
            genericResponse.setData(downloadDocsResponse);
        } catch (Exception e) {
            e.printStackTrace();
            genericResponse.setErrorFlag("T");
            genericResponse.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            genericResponse.setMsg(e.getMessage());
        }
        return genericResponse;
    }

    // ------------------------------------------------------------------
    // v2 endpoints: same business logic, raw bytes instead of base64
    // ------------------------------------------------------------------

    public GenericResponse uploadDocV2(MultipartFile file, String mimeType, String fileName) {
        GenericResponse genericResponse = new GenericResponse();
        try {
            UploadDocResponse uploadDocResponse = ecmService.uploadDocument(file.getBytes(), mimeType, fileName);
            genericResponse.setStatusCode(HttpStatus.OK.value());
            genericResponse.setErrorFlag("F");
            genericResponse.setData(uploadDocResponse);
        } catch (TechnicalException e) {
            e.printStackTrace();
            genericResponse.setErrorFlag("T");
            genericResponse.setStatusCode(HttpStatus.OK.value());
            genericResponse.setMsgCodeAndText(ecmService.getMessage(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            genericResponse.setErrorFlag("T");
            genericResponse.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            genericResponse.setMsg(e.getMessage());
        }

        return genericResponse;
    }

    public ResponseEntity<Object> downloadDocV2(DownloadDocRequest request) {
        try {
            // all validation happens inside downloadDocStream BEFORE the stream is
            // returned, so failures still produce the JSON error responses below;
            // the content itself is then streamed to the client in small chunks
            // without ever being fully buffered in memory
            BinaryDocStream docStream = ecmService.downloadDocStream(request);
            HttpHeaders headers = attachmentHeaders(docStream.getFileName(), docStream.getFileFormat());
            return new ResponseEntity<>(new InputStreamResource(docStream.getInputStream()), headers, HttpStatus.OK);
        } catch (TechnicalException e) {
            e.printStackTrace();
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setErrorFlag("T");
            genericResponse.setStatusCode(HttpStatus.OK.value());
            genericResponse.setMsgCodeAndText(ecmService.getMessage(e.getMessage()));
            return ResponseEntity.status(genericResponse.getStatusCode()).body(genericResponse);
        } catch (Exception e) {
            e.printStackTrace();
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setErrorFlag("T");
            genericResponse.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            genericResponse.setMsg(e.getMessage());
            return ResponseEntity.status(genericResponse.getStatusCode()).body(genericResponse);
        }
    }

    public ResponseEntity<StreamingResponseBody> downloadDocsByIdsV2(List<String> docIdsList) {
        try {
            // validate every document BEFORE streaming starts, so a missing doc
            // still returns a JSON error instead of a truncated zip
            List<ZipDocEntry> entries = ecmService.prepareZipDocs(docIdsList);

            StreamingResponseBody body = outputStream -> {
                try {
                    // each document flows UCM -> zip -> HTTP response in small
                    // chunks; the bundle is never held in memory
                    ecmService.writeZipEntries(entries, outputStream);
                } catch (IOException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IOException("Failed while streaming zip bundle", e);
                }
            };
            return new ResponseEntity<>(body, attachmentHeaders("files.zip", "application/zip"), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setErrorFlag("T");
            genericResponse.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            genericResponse.setMsg(e.getMessage());
            return jsonErrorStream(genericResponse);
        }
    }

    private HttpHeaders attachmentHeaders(String fileName, String fileFormat) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename(fileName == null ? "document" : fileName, StandardCharsets.UTF_8)
                .build());
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(fileFormat);
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        headers.setContentType(mediaType);
        return headers;
    }

    /**
     * Error response for the streaming endpoint: the declared body type must
     * stay StreamingResponseBody, so the JSON error is written as one.
     */
    private ResponseEntity<StreamingResponseBody> jsonErrorStream(GenericResponse genericResponse) {
        byte[] json;
        try {
            json = new ObjectMapper().writeValueAsBytes(genericResponse);
        } catch (Exception e) {
            json = "{\"errorFlag\":\"T\"}".getBytes(StandardCharsets.UTF_8);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentLength(json.length);
        final byte[] payload = json;
        StreamingResponseBody body = outputStream -> outputStream.write(payload);
        return new ResponseEntity<>(body, headers, HttpStatus.valueOf(genericResponse.getStatusCode()));
    }
}
