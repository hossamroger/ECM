package ae.sahabss.ds.ecmservice.exceptions;

public class CheckInException extends TechnicalException {
    public CheckInException(String message)
    {
        super("Error Uploading document");
    } 
}
