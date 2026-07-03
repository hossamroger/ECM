package ae.sahabss.ds.ecmservice.exceptions;

public class UserNotAuthorizedException extends TechnicalException {
    private static final String ERROR_CODE = "ECM-E002";

    public UserNotAuthorizedException() {
        super(ERROR_CODE);
    }
}
