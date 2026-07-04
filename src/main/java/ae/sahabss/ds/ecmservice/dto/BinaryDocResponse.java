package ae.sahabss.ds.ecmservice.dto;

/**
 * Binary counterpart of {@link DownloadDocResponse} for the v2 endpoints:
 * same document metadata, but the content is raw bytes instead of a base64 string.
 */
public class BinaryDocResponse {

    private String fileName;
    private String fileFormat;
    private byte[] content;

    public BinaryDocResponse() {
    }

    public BinaryDocResponse(String fileName, String fileFormat, byte[] content) {
        this.fileName = fileName;
        this.fileFormat = fileFormat;
        this.content = content;
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

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
