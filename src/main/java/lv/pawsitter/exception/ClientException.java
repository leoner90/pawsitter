package lv.pawsitter.exception;


public class ClientException extends RuntimeException {
    public ClientException(String message, RuntimeException ex) {
        super(message);
    }
}
