package no.fint.consumer.exceptions;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private final String message;
    private final String exception;
    private final String cause;

    public static ErrorResponse of(Exception e) {
        return new ErrorResponse(
                toString(e.getMessage()),
                toString(e.getClass()),
                toString(e.getCause())
        );
    }

    private static String toString(Object o) {
        if (o == null)
            return null;
        return String.valueOf(o);
    }
}
