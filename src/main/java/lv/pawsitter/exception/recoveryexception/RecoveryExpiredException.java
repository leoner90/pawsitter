package lv.pawsitter.exception.recoveryexception;

public class RecoveryExpiredException extends RuntimeException {
    public RecoveryExpiredException(String message) {
        super(message);
    }
}
