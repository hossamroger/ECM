package ae.sahabss.ds.ecmservice.controller;

import ae.sahabss.ds.ecmservice.dto.*;
import ae.sahabss.ds.ecmservice.exceptions.TechnicalException;
import ae.sahabss.ds.ecmservice.service.ECMService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

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
            BinaryDocResponse binaryDoc = ecmService.downloadDocBinary(request);
            return binaryResponse(binaryDoc.getContent(), binaryDoc.getFileName(), binaryDoc.getFileFormat());
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

    public ResponseEntity<Object> downloadDocsByIdsV2(List<String> docIdsList) {
        try {
            byte[] zipBytes = ecmService.downloadDocsByIdsAsZip(docIdsList);
            return binaryResponse(zipBytes, "files.zip", "application/zip");
        } catch (Exception e) {
            e.printStackTrace();
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setErrorFlag("T");
            genericResponse.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            genericResponse.setMsg(e.getMessage());
            return ResponseEntity.status(genericResponse.getStatusCode()).body(genericResponse);
        }
    }

    private ResponseEntity<Object> binaryResponse(byte[] content, String fileName, String fileFormat) {
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
        headers.setContentLength(content.length);
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }
}
