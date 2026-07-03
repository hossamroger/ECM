package ae.sahabss.ds.ecmservice.dto;

public class DownloadDocResponse {

    private String fileName;
    private String fileFormat;
    private String encodedDoc;

    public String getEncodedDoc() {
        return encodedDoc;
    }

    public void setEncodedDoc(String encodedDoc) {
        this.encodedDoc = encodedDoc;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }
}
