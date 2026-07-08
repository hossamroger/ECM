package ae.sahabss.ds.ecmservice.controller;

import ae.sahabss.ds.ecmservice.dto.DownloadDocRequest;
import ae.sahabss.ds.ecmservice.dto.GenericResponse;
import ae.sahabss.ds.ecmservice.dto.UploadDocRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

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

    @GetMapping({"/getAllowedDocument/{entityId}", "/v2/getAllowedDocument/{entityId}"})
    public ResponseEntity<GenericResponse> getAllowedUploadDocumentsByEntityId(@PathVariable Integer entityId) {
        GenericResponse genericResponse = ecmServiceControllerWrapper.getAllowedUploadDocumentsByEntityId(entityId);
        return ResponseEntity.status(genericResponse.getStatusCode()).body(genericResponse);
    }

    // ------------------------------------------------------------------
    // v2 endpoints: same business logic, raw bytes instead of base64
    // ------------------------------------------------------------------

    @PostMapping(value = "/v2/uploadDoc", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GenericResponse> uploadDocV2(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mimeType", required = false) String mimeType,
            @RequestParam(value = "fileName", required = false) String fileName)
    {
        String resolvedMimeType = mimeType != null ? mimeType : file.getContentType();
        String resolvedFileName = fileName != null ? fileName : file.getOriginalFilename();
        GenericResponse genericResponse = ecmServiceControllerWrapper.uploadDocV2(file, resolvedMimeType, resolvedFileName);
        return ResponseEntity.status(genericResponse.getStatusCode()).body(genericResponse);
    }

    @PostMapping("/v2/downloadDoc")
    public ResponseEntity<Object> downloadDocV2(@RequestBody DownloadDocRequest request)
    {
        return ecmServiceControllerWrapper.downloadDocV2(request);
    }

    @PostMapping("/v2/downloadDocsByIds")
    public ResponseEntity<StreamingResponseBody> downloadDocsByIdsV2(@RequestBody List<String> docIdsList) {
        return ecmServiceControllerWrapper.downloadDocsByIdsV2(docIdsList);
    }

}
