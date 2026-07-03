package ae.sahabss.ds.ecmservice.domain;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "DS_DOCUMENT_TYPES", schema = "BPM_DATA")
public class DsDocumentTypes {
    private String docTypeCode;
    private String docTypeNameEn;
    private String docTypeNameAr;
    private String belongsTo;
    private Integer entityId;

    @Id
    @Column(name = "DOC_TYPE_CODE", nullable = false)
    public String getDocTypeCode() {
        return docTypeCode;
    }

    public void setDocTypeCode(String docTypeCode) {
        this.docTypeCode = docTypeCode;
    }

    @Basic
    @Column(name = "DOC_TYPE_NAME_EN", nullable = false)
    public String getDocTypeNameEn() {
        return docTypeNameEn;
    }

    public void setDocTypeNameEn(String docTypeNameEn) {
        this.docTypeNameEn = docTypeNameEn;
    }

    @Basic
    @Column(name = "DOC_TYPE_NAME_AR", nullable = false)
    public String getDocTypeNameAr() {
        return docTypeNameAr;
    }

    public void setDocTypeNameAr(String docTypeNameAr) {
        this.docTypeNameAr = docTypeNameAr;
    }

    @Basic
    @Column(name = "BELONGS_TO", length = 1)
    public String getBelongsTo() {
        return belongsTo;
    }

    public void setBelongsTo(String belongsTo) {
        this.belongsTo = belongsTo;
    }

    @Basic
    @Column(name = "ENTITY_ID")
    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DsDocumentTypes that = (DsDocumentTypes) o;
        return Objects.equals(docTypeCode, that.docTypeCode) && Objects.equals(docTypeNameEn, that.docTypeNameEn) && Objects.equals(docTypeNameAr, that.docTypeNameAr) && Objects.equals(belongsTo, that.belongsTo) && Objects.equals(entityId, that.entityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(docTypeCode, docTypeNameEn, docTypeNameAr, belongsTo, entityId);
    }
}
