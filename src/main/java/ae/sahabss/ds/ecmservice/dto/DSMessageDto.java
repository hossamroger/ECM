package ae.sahabss.ds.ecmservice.dto;

public class DSMessageDto {

    private String msgCode;
    private String msgType;
    private String msg;
    private Integer httpStatusCode;

    public DSMessageDto(String msgCode, String msgText, Integer httpStatusCode) {
        this.msgCode = msgCode;
        this.msg = msgText;
        this.httpStatusCode = httpStatusCode;
    }

    public String getMsgCode() {
        return msgCode;
    }

    public void setMsgCode(String msgCode) {
        this.msgCode = msgCode;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }
}
