package ae.sahabss.ds.ecmservice.dto;

import java.io.InputStream;

/**
 * Streaming counterpart of {@link BinaryDocResponse}: the content is an open
 * stream from UCM instead of a fully buffered byte array. The consumer is
 * responsible for closing the stream (Spring closes it after writing the
 * HTTP response when wrapped in an InputStreamResource).
 */
public class BinaryDocStream {

    private final String fileName;
    private final String fileFormat;
    private final InputStream inputStream;

    public BinaryDocStream(String fileName, String fileFormat, InputStream inputStream) {
        this.fileName = fileName;
        this.fileFormat = fileFormat;
        this.inputStream = inputStream;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public InputStream getInputStream() {
        return inputStream;
    }
}
