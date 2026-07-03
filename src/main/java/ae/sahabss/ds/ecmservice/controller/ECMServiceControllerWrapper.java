package ae.sahabss.ds.ecmservice.controller;

import ae.sahabss.ds.ecmservice.dto.*;
import ae.sahabss.ds.ecmservice.exceptions.TechnicalException;
import ae.sahabss.ds.ecmservice.service.ECMService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

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
}
