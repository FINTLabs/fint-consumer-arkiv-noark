package no.fint.consumer.models.dokumentfil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import no.fint.audit.FintAuditService;
import no.fint.cache.exceptions.CacheNotFoundException;
import no.fint.consumer.config.Constants;
import no.fint.consumer.config.ConsumerProps;
import no.fint.consumer.event.ConsumerEventUtil;
import no.fint.consumer.event.SynchronousEvents;
import no.fint.consumer.exceptions.*;
import no.fint.consumer.status.StatusCache;
import no.fint.consumer.utils.ContentDisposition;
import no.fint.consumer.utils.EventResponses;
import no.fint.consumer.utils.RestEndpoints;
import no.fint.event.model.*;
import no.fint.model.arkiv.noark.NoarkActions;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.resource.arkiv.noark.DokumentfilResource;
import no.fint.model.resource.arkiv.noark.DokumentfilResources;
import no.fint.relations.FintRelationsMediaType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
@Api(tags = {"Dokumentfil"})
@CrossOrigin
@RestController
@RequestMapping(name = "Dokumentfil", value = RestEndpoints.DOKUMENTFIL, produces = {FintRelationsMediaType.APPLICATION_HAL_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE})
public class DokumentfilController {

    @Autowired(required = false)
    private DokumentfilCacheService cacheService;

    @Autowired
    private FintAuditService fintAuditService;

    @Autowired
    private DokumentfilLinker linker;

    @Autowired
    private ConsumerProps props;

    @Autowired
    private StatusCache statusCache;

    @Autowired
    private ConsumerEventUtil consumerEventUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SynchronousEvents synchronousEvents;

    @GetMapping("/last-updated")
    public Map<String, String> getLastUpdated(@RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId) {
        if (cacheService == null) {
            throw new CacheDisabledException("Dokumentfil cache is disabled.");
        }
        if (props.isOverrideOrgId() || orgId == null) {
            orgId = props.getDefaultOrgId();
        }
        String lastUpdated = Long.toString(cacheService.getLastUpdated(orgId));
        return ImmutableMap.of("lastUpdated", lastUpdated);
    }

    @GetMapping("/cache/size")
    public ImmutableMap<String, Integer> getCacheSize(@RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId) {
        if (cacheService == null) {
            throw new CacheDisabledException("Dokumentfil cache is disabled.");
        }
        if (props.isOverrideOrgId() || orgId == null) {
            orgId = props.getDefaultOrgId();
        }
        return ImmutableMap.of("size", cacheService.getCacheSize(orgId));
    }

    @GetMapping
    public DokumentfilResources getDokumentfil(
            @RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT, required = false) String client,
            @RequestParam(defaultValue = "0") long sinceTimeStamp,
            @RequestParam(defaultValue = "0") int size,
            @RequestParam(defaultValue = "0") int offset,
            HttpServletRequest request) {
        if (cacheService == null) {
            throw new CacheDisabledException("Dokumentfil cache is disabled.");
        }
        if (props.isOverrideOrgId() || orgId == null) {
            orgId = props.getDefaultOrgId();
        }
        if (client == null) {
            client = props.getDefaultClient();
        }
        log.debug("OrgId: {}, Client: {}", orgId, client);

        Event event = new Event(orgId, Constants.COMPONENT, NoarkActions.GET_ALL_DOKUMENTFIL, client);
        event.setOperation(Operation.READ);
        if (StringUtils.isNotBlank(request.getQueryString())) {
            event.setQuery("?" + request.getQueryString());
        }
        fintAuditService.audit(event);
        fintAuditService.audit(event, Status.CACHE);

        Stream<DokumentfilResource> resources;
        if (size > 0 && offset >= 0 && sinceTimeStamp > 0) {
            resources = cacheService.streamSliceSince(orgId, sinceTimeStamp, offset, size);
        } else if (size > 0 && offset >= 0) {
            resources = cacheService.streamSlice(orgId, offset, size);
        } else if (sinceTimeStamp > 0) {
            resources = cacheService.streamSince(orgId, sinceTimeStamp);
        } else {
            resources = cacheService.streamAll(orgId);
        }

        fintAuditService.audit(event, Status.CACHE_RESPONSE, Status.SENT_TO_CLIENT);

        return linker.toResources(resources, offset, size, cacheService.getCacheSize(orgId));
    }


    @GetMapping("/systemid/{id:.+}")
    public ResponseEntity getDokumentfilBySystemId(
            @PathVariable String id,
            @RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT, required = false) String client) throws InterruptedException {
        if (props.isOverrideOrgId() || orgId == null) {
            orgId = props.getDefaultOrgId();
        }
        if (client == null) {
            client = props.getDefaultClient();
        }
        log.debug("systemId: {}, OrgId: {}, Client: {}", id, orgId, client);

        Event event = new Event(orgId, Constants.COMPONENT, NoarkActions.GET_DOKUMENTFIL, client);
        event.setOperation(Operation.READ);
        event.setQuery("systemId/" + id);

        if (cacheService != null) {
            fintAuditService.audit(event);
            fintAuditService.audit(event, Status.CACHE);

            Optional<DokumentfilResource> dokumentfil = cacheService.getDokumentfilBySystemId(orgId, id);

            fintAuditService.audit(event, Status.CACHE_RESPONSE, Status.SENT_TO_CLIENT);

            return ResponseEntity.ok(dokumentfil.map(linker::toResource).orElseThrow(() -> new EntityNotFoundException(id)));

        } else {
            BlockingQueue<Event> queue = synchronousEvents.register(event);
            consumerEventUtil.send(event);

            Event response = EventResponses.handle(queue.poll(5, TimeUnit.MINUTES));

            if (response.getData() == null ||
                    response.getData().isEmpty()) throw new EntityNotFoundException(id);

            DokumentfilResource dokumentfil = objectMapper.convertValue(response.getData().get(0), DokumentfilResource.class);

            final ResponseEntity responseEntity = getResponseEntity(dokumentfil, HttpStatus.OK, null);

            fintAuditService.audit(response, Status.SENT_TO_CLIENT);

            return responseEntity;
        }
    }

    private ResponseEntity getResponseEntity(DokumentfilResource dokumentfil, HttpStatus status, URI location) {
        byte[] decoded = Base64.getDecoder().decode(dokumentfil.getData());

        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename(dokumentfil.getFilnavn(), StandardCharsets.UTF_8)
                .build();
        ResponseEntity.BodyBuilder builder = ResponseEntity
                .status(status)
                .header(HttpHeaders.CONTENT_TYPE, dokumentfil.getFormat())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
        if (location != null) {
            builder = builder.location(location);
        }
        return builder
                .body(decoded);
    }

    // Writable class
    @GetMapping("/status/{id}")
    public ResponseEntity getStatus(
            @PathVariable String id,
            @RequestHeader(HeaderConstants.ORG_ID) String orgId,
            @RequestHeader(HeaderConstants.CLIENT) String client) {
        log.debug("/status/{} for {} from {}", id, orgId, client);
        if (!statusCache.containsKey(id)) {
            return ResponseEntity.status(HttpStatus.GONE).build();
        }
        Event event = statusCache.get(id);
        log.debug("Event: {}", event);
        log.trace("Data: {}", event.getData());
        if (!event.getOrgId().equals(orgId)) {
            return ResponseEntity.badRequest().body(new EventResponse() {
                {
                    setMessage("Invalid OrgId");
                }
            });
        }
        if (event.getResponseStatus() == null) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }
        DokumentfilResource result;
        URI location;
        switch (event.getResponseStatus()) {
            case ACCEPTED:
                if (event.getOperation() == Operation.VALIDATE) {
                    fintAuditService.audit(event, Status.SENT_TO_CLIENT);
                    return ResponseEntity.ok(event.getResponse());
                }
                result = objectMapper.convertValue(event.getData().get(0), DokumentfilResource.class);
                location = UriComponentsBuilder.fromUriString(linker.getSelfHref(result)).build().toUri();
                event.setMessage(location.toString());
                fintAuditService.audit(event, Status.SENT_TO_CLIENT);
                if (props.isUseCreated())
                    return getResponseEntity(result, HttpStatus.CREATED, location);
                return getResponseEntity(result, HttpStatus.SEE_OTHER, location);
            case ERROR:
                fintAuditService.audit(event, Status.SENT_TO_CLIENT);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(event.getResponse());
            case CONFLICT:
                fintAuditService.audit(event, Status.SENT_TO_CLIENT);
                result = objectMapper.convertValue(event.getData().get(0), DokumentfilResource.class);
                location = UriComponentsBuilder.fromUriString(linker.getSelfHref(result)).build().toUri();
                return getResponseEntity(result, HttpStatus.CONFLICT, location);
            case REJECTED:
                fintAuditService.audit(event, Status.SENT_TO_CLIENT);
                return ResponseEntity.badRequest().body(event.getResponse());
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(event.getResponse());
    }

    @PostMapping
    public ResponseEntity postDokumentfil(
            @RequestHeader(name = HeaderConstants.ORG_ID) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT) String client,
            @RequestHeader(name = HttpHeaders.CONTENT_TYPE) String format,
            @RequestHeader(name = HttpHeaders.CONTENT_DISPOSITION) String disposition,
            @RequestBody byte[] body
    ) {
        log.debug("postDokumentfil, OrgId: {}, Client: {}", orgId, client);
        return updateDokumentfil(Operation.CREATE, orgId, client, format, disposition, body);
    }

    private ResponseEntity updateDokumentfil(Operation operation, String orgId, String client, String format, String disposition, byte[] body) {
        ContentDisposition contentDisposition = ContentDisposition.parse(disposition);
        DokumentfilResource dokument = new DokumentfilResource();
        dokument.setData(Base64.getEncoder().encodeToString(body));
        dokument.setFilnavn(contentDisposition.getFilename());
        dokument.setFormat(format);
        linker.mapLinks(dokument);
        Event event = new Event(orgId, Constants.COMPONENT, NoarkActions.UPDATE_DOKUMENTFIL, client);
        if (dokument.getSystemId() == null || StringUtils.isBlank(dokument.getSystemId().getIdentifikatorverdi())) {
            dokument.setSystemId(new Identifikator() {{
                setIdentifikatorverdi(event.getCorrId());
            }});
        }
        event.addObject(objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS).convertValue(dokument, Map.class));
        event.setOperation(operation);
        consumerEventUtil.send(event);

        statusCache.put(event.getCorrId(), event);
        URI location = UriComponentsBuilder.fromUriString(linker.self()).path("status/{id}").buildAndExpand(event.getCorrId()).toUri();
        return ResponseEntity.status(HttpStatus.ACCEPTED).location(location).build();
    }

    @PutMapping("/systemid/{id:.+}")
    public ResponseEntity putDokumentfilBySystemId(
            @PathVariable String id,
            @RequestHeader(name = HeaderConstants.ORG_ID) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT) String client,
            @RequestHeader(name = HttpHeaders.CONTENT_TYPE) String format,
            @RequestHeader(name = HttpHeaders.CONTENT_DISPOSITION) String disposition,
            @RequestBody byte[] body
    ) {
        log.debug("putDokumentfilBySystemId {}, OrgId: {}, Client: {}", id, orgId, client);
        return updateDokumentfil(Operation.UPDATE, orgId, client, format, disposition, body);
    }

    //
    // Exception handlers
    //
    @ExceptionHandler(EventResponseException.class)
    public ResponseEntity handleEventResponseException(EventResponseException e) {
        return ResponseEntity.status(e.getStatus()).body(e.getResponse());
    }

    @ExceptionHandler(UpdateEntityMismatchException.class)
    public ResponseEntity handleUpdateEntityMismatch(Exception e) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(e));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity handleEntityNotFound(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(e));
    }

    @ExceptionHandler(CreateEntityMismatchException.class)
    public ResponseEntity handleCreateEntityMismatch(Exception e) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(e));
    }

    @ExceptionHandler(EntityFoundException.class)
    public ResponseEntity handleEntityFound(Exception e) {
        return ResponseEntity.status(HttpStatus.FOUND).body(ErrorResponse.of(e));
    }

    @ExceptionHandler(CacheDisabledException.class)
    public ResponseEntity handleBadRequest(Exception e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ErrorResponse.of(e));
    }

    @ExceptionHandler(UnknownHostException.class)
    public ResponseEntity handleUnkownHost(Exception e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ErrorResponse.of(e));
    }

    @ExceptionHandler(CacheNotFoundException.class)
    public ResponseEntity handleCacheNotFound(Exception e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ErrorResponse.of(e));
    }

}