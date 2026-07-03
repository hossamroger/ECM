package ae.sahabss.ds.ecmservice.controller;

import ae.sahabss.ds.ecmservice.dto.DownloadDocRequest;
import ae.sahabss.ds.ecmservice.dto.GenericResponse;
import ae.sahabss.ds.ecmservice.dto.UploadDocRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ECMServiceController {

    private final ECMServiceControllerWrapper ecmServiceControllerWrapper;

    public ECMServiceController(ECMServiceControllerWrapper ecmServiceControllerWrapper) {
        this.ecmServiceControllerWrapper = ecmServiceControllerWrapper;
    }

    @PostMapping("/uploadDoc")
    public ResponseEntity<GenericResponse> uploadDoc(@RequestBody UploadDocRequest request)
    {
        GenericResponse genericResponse = ecmServiceControllerWrapper.uploadDoc(request);
        return ResponseEntity.status(genericResponse.getStatusCode()).body(genericResponse);
    }

    @PostMapping("/downloadDoc")
    public ResponseEntity<GenericResponse> downloadDoc(@RequestBody DownloadDocRequest request)
    {
        GenericResponse genericResponse = ecmServiceControllerWrapper.downloadDoc(request);
        return ResponseEntity.status(genericResponse.getStatusCode()).body(genericResponse);
    }

    @PostMapping("/downloadDocsByIds")
    public ResponseEntity<GenericResponse> downloadDocsByIds(@RequestBody List<String> docIdsList) {
        GenericResponse genericResponse = ecmServiceControllerWrapper.downloadDocsByIds(docIdsList);
        return ResponseEntity.status(genericResponse.getStatusCode()).body(genericResponse);
    }

    @GetMapping("/getAllowedDocument/{entityId}")
    public ResponseEntity<GenericResponse> getAllowedUploadDocumentsByEntityId(@PathVariable Integer entityId) {
        GenericResponse genericResponse = ecmServiceControllerWrapper.getAllowedUploadDocumentsByEntityId(entityId);
        return ResponseEntity.status(genericResponse.getStatusCode()).body(genericResponse);
    }

}
