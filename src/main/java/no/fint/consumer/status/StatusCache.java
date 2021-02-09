package no.fint.consumer.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import no.fint.audit.FintAuditService;
import no.fint.consumer.config.ConsumerProps;
import no.fint.event.model.Event;
import no.fint.event.model.EventResponse;
import no.fint.event.model.Operation;
import no.fint.event.model.Status;
import no.fint.model.resource.FintLinks;
import no.fint.relations.FintLinker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.Objects;

@Slf4j
@Component
public class StatusCache {

    @Autowired
    private ConsumerProps props;

    @Autowired
    private FintAuditService fintAuditService;

    @Autowired
    private ObjectMapper objectMapper;

    private Cache<String, Event> cache;

    @Value("${fint.consumer.status.cache:expireAfterWrite=30m}")
    private String cacheSpec;

    @PostConstruct
    public void init() {
        cache = CacheBuilder.from(cacheSpec).build();
    }

    public boolean containsKey(String id) {
        return Objects.nonNull(cache.getIfPresent(id));
    }

    public Event get(String id) {
        return cache.getIfPresent(id);
    }

    public void put(String corrId, Event event) {
        cache.put(corrId, event);
    }

    public <T extends FintLinks> ResponseEntity handleStatusRequest(String id, String orgId, FintLinker<T> linker, Class<T> valueType) {
        if (!containsKey(id)) {
            return ResponseEntity.status(HttpStatus.GONE).build();
        }
        Event event = get(id);
        log.debug("Event: {}", event);
        log.trace("Data: {}", event.getData());
        if (!event.getOrgId().equals(orgId)) {
            return ResponseEntity.badRequest().body(new EventResponse() { { setMessage("Invalid OrgId"); } } );
        }
        if (event.getResponseStatus() == null) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }
        T result;
        switch (event.getResponseStatus()) {
            case ACCEPTED:
                if (event.getOperation() == Operation.VALIDATE) {
                    fintAuditService.audit(event, Status.SENT_TO_CLIENT);
                    return ResponseEntity.ok(event.getResponse());
                }
                result = objectMapper.convertValue(event.getData().get(0), valueType);
                URI location = UriComponentsBuilder.fromUriString(linker.getSelfHref(result)).build().toUri();
                event.setMessage(location.toString());
                fintAuditService.audit(event, Status.SENT_TO_CLIENT);
                if (props.isUseCreated())
                    return ResponseEntity.created(location).body(linker.toResource(result));
                return ResponseEntity.status(HttpStatus.SEE_OTHER).location(location).body(linker.toResource(result));
            case ERROR:
                fintAuditService.audit(event, Status.SENT_TO_CLIENT);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(event.getResponse());
            case CONFLICT:
                fintAuditService.audit(event, Status.SENT_TO_CLIENT);
                result = objectMapper.convertValue(event.getData().get(0), valueType);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(linker.toResource(result));
            case REJECTED:
                fintAuditService.audit(event, Status.SENT_TO_CLIENT);
                return ResponseEntity.status(getStatus(event.getStatusCode())).body(event.getResponse());
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(event.getResponse());

    }

    private HttpStatus getStatus(String statusCode) {
        if (StringUtils.isBlank(statusCode)) {
            return HttpStatus.BAD_REQUEST;
        }
        try {
            return HttpStatus.valueOf(statusCode);
        } catch (RuntimeException e) {
            return HttpStatus.BAD_REQUEST;
        }
    }

}
