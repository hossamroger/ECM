package ae.sahabss.ds.ecmservice.domain;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "DS_MESSAGES", schema = "BPM_DATA", catalog = "")
public class DsMessagesEntity {
    private Integer msgId;
    private String msgCode;
    private String lang;
    private String msgText;
    private String msgType;
    private Integer httpStatusCode;

    @Id
    @Column(name = "MSG_ID", nullable = false, precision = 0)
    public Integer getMsgId() {
        return msgId;
    }

    public void setMsgId(Integer msgId) {
        this.msgId = msgId;
    }

    @Basic
    @Column(name = "MSG_CODE", nullable = false, length = 10)
    public String getMsgCode() {
        return msgCode;
    }

    public void setMsgCode(String msgCode) {
        this.msgCode = msgCode;
    }

    @Basic
    @Column(name = "LANG", nullable = false, length = 2)
    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    @Basic
    @Column(name = "MSG_TEXT", nullable = false, length = 255)
    public String getMsgText() {
        return msgText;
    }

    public void setMsgText(String msgText) {
        this.msgText = msgText;
    }

    @Basic
    @Column(name = "MSG_TYPE", nullable = false, length = 1)
    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DsMessagesEntity that = (DsMessagesEntity) o;
        return Objects.equals(msgId, that.msgId) && Objects.equals(msgCode, that.msgCode) && Objects.equals(lang, that.lang) && Objects.equals(msgText, that.msgText) && Objects.equals(msgType, that.msgType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(msgId, msgCode, lang, msgText, msgType);
    }

    @Basic
    @Column(name = "HTTP_STATUS_CODE", nullable = true, precision = 0)
    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }
}
