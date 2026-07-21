package lv.pawsitter.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(long id) {
        super("User with id " + id + " is not found.");
    }

    public UserNotFoundException(String email) {
        super("User with email " + email + " is not found.");
    }
}
