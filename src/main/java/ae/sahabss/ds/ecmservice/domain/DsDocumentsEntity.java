package ae.sahabss.ds.ecmservice.domain;

import javax.persistence.*;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "DS_DOCUMENTS", schema = "BPM_DATA")
public class DsDocumentsEntity {
    private Integer dsDocumentId;
    private String docId;
    private String fileName;
    private Timestamp createdDate;
    private String mimeType;

    @Id
    @Column(name = "DS_DOCUMENT_ID", nullable = false, precision = 0)
    public Integer getDsDocumentId() {
        return dsDocumentId;
    }

    public void setDsDocumentId(Integer dsDocumentId) {
        this.dsDocumentId = dsDocumentId;
    }

    @Basic
    @Column(name = "DOC_ID", nullable = false, length = 255)
    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    @Basic
    @Column(name = "FILE_NAME", nullable = false, length = 255)
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Basic
    @Column(name = "CREATED_DATE", nullable = false)
    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    @Column(name = "MIME_TYPE", length = 100)
    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DsDocumentsEntity that = (DsDocumentsEntity) o;
        return Objects.equals(dsDocumentId, that.dsDocumentId) && Objects.equals(docId, that.docId) && Objects.equals(fileName, that.fileName) && Objects.equals(createdDate, that.createdDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dsDocumentId, docId, fileName, createdDate);
    }
}
