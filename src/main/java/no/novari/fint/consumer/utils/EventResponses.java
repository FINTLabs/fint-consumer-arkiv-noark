package no.novari.fint.consumer.utils;

import no.novari.fint.consumer.exceptions.EventResponseException;
import no.fint.event.model.Event;
import no.fint.event.model.EventResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

public class EventResponses {

    public static Event<?> handle(Event<?> event) {
        if (event == null || event.getResponseStatus() == null)
            throw new EventResponseException(HttpStatus.INTERNAL_SERVER_ERROR, new EventResponse() {{
                setMessage("No response from adapter.");
            }});
        switch (event.getResponseStatus()) {
            case ERROR:
                throw new EventResponseException(HttpStatus.INTERNAL_SERVER_ERROR, event.getResponse());
            case REJECTED:
                if (StringUtils.isBlank(event.getStatusCode())) {
                    throw new EventResponseException(HttpStatus.BAD_REQUEST, event.getResponse());
                }
                switch (event.getStatusCode()) {
                    case "GONE":
                        throw new EventResponseException(HttpStatus.GONE, event.getResponse());
                    case "NOT_FOUND":
                        throw new EventResponseException(HttpStatus.NOT_FOUND, event.getResponse());
                    default:
                        throw new EventResponseException(HttpStatus.BAD_REQUEST, event.getResponse());
                }
            case CONFLICT:
                throw new EventResponseException(HttpStatus.CONFLICT, event.getResponse());
        }
        return event;
    }
}
