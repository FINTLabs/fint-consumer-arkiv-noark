package no.fint.consumer.exceptions;

public class CacheDisabledException extends RuntimeException {
    public CacheDisabledException(String message) {
        super(message);
    }
}
