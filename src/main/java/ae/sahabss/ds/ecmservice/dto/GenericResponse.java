package ae.sahabss.ds.ecmservice.dto;

public class GenericResponse {

    private Object data;
    private int statusCode;
    private String errorFlag;
    private String msg;
    private String msgCode;

    public Object getData() {
        return data;
    }

    public String getMsgCode() {
        return msgCode;
    }

    public void setMsgCode(String msgCode) {
        this.msgCode = msgCode;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
    public void setMsgCodeAndText(DSMessageDto dsMessageDto) {
        this.msgCode = dsMessageDto.getMsgCode();
        this.msg = dsMessageDto.getMsg();
    }
    public String getErrorFlag() {
        return errorFlag;
    }

    public void setErrorFlag(String errorFlag) {
        this.errorFlag = errorFlag;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
