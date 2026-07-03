package ae.sahabss.ds.ecmservice.dto;

import ae.sahabss.ds.ecmservice.domain.DsDocumentTypes;

import java.util.List;

public class AllowedUploadDocumentsResponse {

    private List<DsDocumentTypes> documentsList;

    public AllowedUploadDocumentsResponse(List<DsDocumentTypes> documentsList) {
        this.documentsList = documentsList;
    }

    public List<DsDocumentTypes> getDocumentsList() {
        return documentsList;
    }

    public void setDocumentsList(List<DsDocumentTypes> documentsList) {
        this.documentsList = documentsList;
    }
}
