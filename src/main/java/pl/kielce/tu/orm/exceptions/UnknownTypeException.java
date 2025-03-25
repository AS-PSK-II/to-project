package pl.kielce.tu.orm.exceptions;

public class UnknownTypeException extends Exception {
    public UnknownTypeException(String message) {
        super(message);
    }

    public UnknownTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
