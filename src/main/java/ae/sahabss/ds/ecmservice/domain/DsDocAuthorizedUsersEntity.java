package ae.sahabss.ds.ecmservice.domain;

import javax.persistence.*;
import java.math.BigInteger;
import java.util.Objects;

@Entity
@Table(name = "DS_DOC_AUTHORIZED_USERS", schema = "BPM_DATA", catalog = "")
public class DsDocAuthorizedUsersEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "DOC_AUTHORIZED_USER_ID", nullable = false, precision = 0)
    private Integer docAuthorizedUserId;
    @Basic
    @Column(name = "DOC_ID", nullable = false, length = 20)
    private String docId;
    @Basic
    @Column(name = "EID", nullable = true, length = 20)
    private String eid;
    @Basic
    @Column(name = "MOBILE_NO", nullable = true, length = 20)
    private String mobileNo;
    @Basic
    @Column(name = "USER_ID", nullable = true, length = 20)
    private String userId;


    public DsDocAuthorizedUsersEntity(String docId, String eid, String mobileNo, String userId) {
        this.docId = docId;
        this.eid = eid;
        this.mobileNo = mobileNo;
        this.userId = userId;
    }

    public DsDocAuthorizedUsersEntity() {
    }

    public Integer getDocAuthorizedUserId() {
        return docAuthorizedUserId;
    }

    public void setDocAuthorizedUserId(Integer docAuthorizedUserId) {
        this.docAuthorizedUserId = docAuthorizedUserId;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getEid() {
        return eid;
    }

    public void setEid(String eid) {
        this.eid = eid;
    }

    public String getMobileNo() {
        return mobileNo;
    }

    public void setMobileNo(String mobileNo) {
        this.mobileNo = mobileNo;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DsDocAuthorizedUsersEntity that = (DsDocAuthorizedUsersEntity) o;
        return Objects.equals(docAuthorizedUserId, that.docAuthorizedUserId) && Objects.equals(docId, that.docId) && Objects.equals(eid, that.eid) && Objects.equals(mobileNo, that.mobileNo) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(docAuthorizedUserId, docId, eid, mobileNo, userId);
    }
}
