package ae.sahabss.ds.ecmservice.dto;

/**
 * A document that has been validated (exists in UCM) and is ready to be
 * streamed into a zip bundle under the given entry name.
 */
public class ZipDocEntry {

    private final String docId;
    private final String entryName;

    public ZipDocEntry(String docId, String entryName) {
        this.docId = docId;
        this.entryName = entryName;
    }

    public String getDocId() {
        return docId;
    }

    public String getEntryName() {
        return entryName;
    }
}
