package lv.pawsitter.exception;

public class InvalidBookingOperationException extends RuntimeException
{
    public InvalidBookingOperationException(String message)
    {
        super(message);
    }
}
