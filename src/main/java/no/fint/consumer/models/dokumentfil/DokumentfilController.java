package no.fint.consumer.models.dokumentfil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import no.fint.consumer.utils.ContentDisposition;
import no.novari.fint.model.felles.kompleksedatatyper.Identifikator;
import org.apache.commons.lang3.StringUtils;

import no.fint.audit.FintAuditService;

import no.fint.cache.exceptions.*;
import no.fint.consumer.config.Constants;
import no.fint.consumer.config.ConsumerProps;
import no.fint.consumer.event.ConsumerEventUtil;
import no.fint.consumer.event.SynchronousEvents;
import no.fint.consumer.exceptions.*;
import no.fint.consumer.status.StatusCache;
import no.fint.consumer.utils.EventResponses;
import no.fint.consumer.utils.RestEndpoints;
import no.fint.antlr.FintFilterService;

import no.fint.event.model.*;

import no.fint.relations.FintRelationsMediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.UnknownHostException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import no.novari.fint.model.resource.arkiv.noark.DokumentfilResource;
import no.novari.fint.model.resource.arkiv.noark.DokumentfilResources;
import no.novari.fint.model.arkiv.noark.NoarkActions;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Api(tags = {"Dokumentfil"})
@CrossOrigin
@RestController
@RequestMapping(name = "Dokumentfil", value = RestEndpoints.DOKUMENTFIL, produces = {FintRelationsMediaType.APPLICATION_HAL_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE})
public class DokumentfilController {

    private static final String ODATA_FILTER_QUERY_OPTION = "$filter=";

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

    @Autowired
    private FintFilterService fintFilterService;

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
            @RequestParam(required = false) String $filter,
            HttpServletRequest request) throws InterruptedException {
        if (cacheService == null) {
            if (StringUtils.isNotBlank($filter)) {
                return getDokumentfilByOdataFilter(client, orgId, $filter);
            }
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
    
    @PostMapping("/$query")
    public DokumentfilResources getDokumentfilByQuery(
            @RequestHeader(name = HeaderConstants.ORG_ID, required = false)   String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT, required = false) String client,
            @RequestParam(defaultValue = "0") long sinceTimeStamp,
            @RequestParam(defaultValue = "0") int  size,
            @RequestParam(defaultValue = "0") int  offset,
            @RequestBody(required = false) String query,
            HttpServletRequest request
    ) throws InterruptedException {
        return getDokumentfil(orgId, client, sinceTimeStamp, size, offset, query, request);
    }

    private DokumentfilResources getDokumentfilByOdataFilter(
        String client, String orgId, String $filter
    ) throws InterruptedException {
        if (!fintFilterService.validate($filter))
            throw new IllegalArgumentException("OData Filter is not valid");
    
        if (props.isOverrideOrgId() || orgId == null) orgId = props.getDefaultOrgId();
        if (client == null) client = props.getDefaultClient();
    
        Event event = new Event(
                orgId, Constants.COMPONENT,
                NoarkActions.GET_DOKUMENTFIL, client);
        event.setOperation(Operation.READ);
        event.setQuery(ODATA_FILTER_QUERY_OPTION.concat($filter));
    
        BlockingQueue<Event> queue = synchronousEvents.register(event);
        consumerEventUtil.send(event);
    
        Event response = EventResponses.handle(queue.poll(5, TimeUnit.MINUTES));
        if (response.getData() == null || response.getData().isEmpty())
            return new DokumentfilResources();
    
        ArrayList<DokumentfilResource> list = objectMapper.convertValue(
                response.getData(),
                new TypeReference<ArrayList<DokumentfilResource>>() {});
        fintAuditService.audit(response, Status.SENT_TO_CLIENT);
        list.forEach(r -> linker.mapAndResetLinks(r));
        return linker.toResources(list);
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

    // Writable class
    @GetMapping("/status/{id}")
    public ResponseEntity getStatus(
            @PathVariable String id,
            @RequestHeader(HeaderConstants.ORG_ID) String orgId,
            @RequestHeader(HeaderConstants.CLIENT) String client) {
        log.debug("/status/{} for {} from {}", id, orgId, client);
        return statusCache.handleStatusRequest(id, orgId, linker, DokumentfilResource.class);
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

