package no.novari.fint.consumer.models.sak;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import no.fint.audit.FintAuditService;

import no.fint.cache.exceptions.*;
import no.novari.fint.consumer.config.Constants;
import no.novari.fint.consumer.config.ConsumerProps;
import no.novari.fint.consumer.event.ConsumerEventUtil;
import no.novari.fint.consumer.event.SynchronousEvents;
import no.novari.fint.consumer.exceptions.*;
import no.novari.fint.consumer.status.StatusCache;
import no.novari.fint.consumer.utils.EventResponses;
import no.novari.fint.consumer.utils.RestEndpoints;
import no.fint.antlr.FintFilterService;

import no.fint.event.model.*;

import no.novari.fint.relations.FintRelationsMediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.UnknownHostException;

import java.util.Map;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import no.novari.fint.model.resource.arkiv.noark.SakResource;
import no.novari.fint.model.resource.arkiv.noark.SakResources;
import no.novari.fint.model.arkiv.noark.NoarkActions;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Api(tags = {"Sak"})
@CrossOrigin
@RestController
@RequestMapping(name = "Sak", value = RestEndpoints.SAK, produces = {FintRelationsMediaType.APPLICATION_HAL_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE})
public class SakController {

    private static final String ODATA_FILTER_QUERY_OPTION = "$filter=";

    @Autowired(required = false)
    private SakCacheService cacheService;

    @Autowired
    private FintAuditService fintAuditService;

    @Autowired
    private SakLinker linker;

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
            throw new CacheDisabledException("Sak cache is disabled.");
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
            throw new CacheDisabledException("Sak cache is disabled.");
        }
        if (props.isOverrideOrgId() || orgId == null) {
            orgId = props.getDefaultOrgId();
        }
        return ImmutableMap.of("size", cacheService.getCacheSize(orgId));
    }

    @GetMapping
    public SakResources getSak(
            @RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT, required = false) String client,
            @RequestParam(defaultValue = "0") long sinceTimeStamp,
            @RequestParam(defaultValue = "0") int size,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String $filter,
            HttpServletRequest request) throws InterruptedException {
        if (cacheService == null) {
            if (StringUtils.isNotBlank($filter)) {
                return getSakByOdataFilter(client, orgId, $filter);
            }
            throw new CacheDisabledException("Sak cache is disabled.");
        }
        if (props.isOverrideOrgId() || orgId == null) {
            orgId = props.getDefaultOrgId();
        }
        if (client == null) {
            client = props.getDefaultClient();
        }
        log.debug("OrgId: {}, Client: {}", orgId, client);

        Event event = new Event(orgId, Constants.COMPONENT, NoarkActions.GET_ALL_SAK, client);
        event.setOperation(Operation.READ);
        if (StringUtils.isNotBlank(request.getQueryString())) {
            event.setQuery("?" + request.getQueryString());
        }
        fintAuditService.audit(event);
        fintAuditService.audit(event, Status.CACHE);

        Stream<SakResource> resources;
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
    public SakResources getSakByQuery(
            @RequestHeader(name = HeaderConstants.ORG_ID, required = false)   String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT, required = false) String client,
            @RequestParam(defaultValue = "0") long sinceTimeStamp,
            @RequestParam(defaultValue = "0") int  size,
            @RequestParam(defaultValue = "0") int  offset,
            @RequestBody(required = false) String query,
            HttpServletRequest request
    ) throws InterruptedException {
        return getSak(orgId, client, sinceTimeStamp, size, offset, query, request);
    }

    private SakResources getSakByOdataFilter(
        String client, String orgId, String $filter
    ) throws InterruptedException {
        if (!fintFilterService.validate($filter))
            throw new IllegalArgumentException("OData Filter is not valid");
    
        if (props.isOverrideOrgId() || orgId == null) orgId = props.getDefaultOrgId();
        if (client == null) client = props.getDefaultClient();
    
        Event event = new Event(
                orgId, Constants.COMPONENT,
                NoarkActions.GET_SAK, client);
        event.setOperation(Operation.READ);
        event.setQuery(ODATA_FILTER_QUERY_OPTION.concat($filter));
    
        BlockingQueue<Event> queue = synchronousEvents.register(event);
        consumerEventUtil.send(event);
    
        Event response = EventResponses.handle(queue.poll(5, TimeUnit.MINUTES));
        if (response.getData() == null || response.getData().isEmpty())
            return new SakResources();
    
        ArrayList<SakResource> list = objectMapper.convertValue(
                response.getData(),
                new TypeReference<ArrayList<SakResource>>() {});
        fintAuditService.audit(response, Status.SENT_TO_CLIENT);
        list.forEach(r -> linker.mapAndResetLinks(r));
        return linker.toResources(list);
    }


    @GetMapping("/mappeid/{id:.+}")
    public SakResource getSakByMappeId(
            @PathVariable String id,
            @RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT, required = false) String client) throws InterruptedException {
        if (props.isOverrideOrgId() || orgId == null) {
            orgId = props.getDefaultOrgId();
        }
        if (client == null) {
            client = props.getDefaultClient();
        }
        log.debug("mappeId: {}, OrgId: {}, Client: {}", id, orgId, client);

        Event event = new Event(orgId, Constants.COMPONENT, NoarkActions.GET_SAK, client);
        event.setOperation(Operation.READ);
        event.setQuery("mappeId/" + id);

        if (cacheService != null) {
            fintAuditService.audit(event);
            fintAuditService.audit(event, Status.CACHE);

            Optional<SakResource> sak = cacheService.getSakByMappeId(orgId, id);

            fintAuditService.audit(event, Status.CACHE_RESPONSE, Status.SENT_TO_CLIENT);

            return sak.map(linker::toResource).orElseThrow(() -> new EntityNotFoundException(id));

        } else {
            BlockingQueue<Event> queue = synchronousEvents.register(event);
            consumerEventUtil.send(event);

            Event response = EventResponses.handle(queue.poll(5, TimeUnit.MINUTES));

            if (response.getData() == null ||
                    response.getData().isEmpty()) throw new EntityNotFoundException(id);

            SakResource sak = objectMapper.convertValue(response.getData().get(0), SakResource.class);

            fintAuditService.audit(response, Status.SENT_TO_CLIENT);

            return linker.mapAndResetLinks(sak);
        }    
    }

    @GetMapping("/systemid/{id:.+}")
    public SakResource getSakBySystemId(
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

        Event event = new Event(orgId, Constants.COMPONENT, NoarkActions.GET_SAK, client);
        event.setOperation(Operation.READ);
        event.setQuery("systemId/" + id);

        if (cacheService != null) {
            fintAuditService.audit(event);
            fintAuditService.audit(event, Status.CACHE);

            Optional<SakResource> sak = cacheService.getSakBySystemId(orgId, id);

            fintAuditService.audit(event, Status.CACHE_RESPONSE, Status.SENT_TO_CLIENT);

            return sak.map(linker::toResource).orElseThrow(() -> new EntityNotFoundException(id));

        } else {
            BlockingQueue<Event> queue = synchronousEvents.register(event);
            consumerEventUtil.send(event);

            Event response = EventResponses.handle(queue.poll(5, TimeUnit.MINUTES));

            if (response.getData() == null ||
                    response.getData().isEmpty()) throw new EntityNotFoundException(id);

            SakResource sak = objectMapper.convertValue(response.getData().get(0), SakResource.class);

            fintAuditService.audit(response, Status.SENT_TO_CLIENT);

            return linker.mapAndResetLinks(sak);
        }    
    }



    // Writable class
    @GetMapping("/status/{id}")
    public ResponseEntity getStatus(
            @PathVariable String id,
            @RequestHeader(HeaderConstants.ORG_ID) String orgId,
            @RequestHeader(HeaderConstants.CLIENT) String client) {
        log.debug("/status/{} for {} from {}", id, orgId, client);
        return statusCache.handleStatusRequest(id, orgId, linker, SakResource.class);
    }

    @PostMapping
    public ResponseEntity postSak(
            @RequestHeader(name = HeaderConstants.ORG_ID) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT) String client,
            @RequestBody SakResource body,
            @RequestParam(name = "validate", required = false) boolean validate
    ) {
        log.debug("postSak, Validate: {}, OrgId: {}, Client: {}", validate, orgId, client);
        log.trace("Body: {}", body);
        linker.mapLinks(body);
        Event event = new Event(orgId, Constants.COMPONENT, NoarkActions.UPDATE_SAK, client);
        event.addObject(objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS).convertValue(body, Map.class));
        event.setOperation(validate ? Operation.VALIDATE : Operation.CREATE);
        consumerEventUtil.send(event);

        statusCache.put(event.getCorrId(), event);

        URI location = UriComponentsBuilder.fromUriString(linker.self()).path("status/{id}").buildAndExpand(event.getCorrId()).toUri();
        return ResponseEntity.status(HttpStatus.ACCEPTED).location(location).build();
    }

  
    @PutMapping("/mappeid/{id:.+}")
    public ResponseEntity putSakByMappeId(
            @PathVariable String id,
            @RequestHeader(name = HeaderConstants.ORG_ID) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT) String client,
            @RequestBody SakResource body
    ) {
        log.debug("putSakByMappeId {}, OrgId: {}, Client: {}", id, orgId, client);
        log.trace("Body: {}", body);
        linker.mapLinks(body);
        Event event = new Event(orgId, Constants.COMPONENT, NoarkActions.UPDATE_SAK, client);
        event.setQuery("mappeid/" + id);
        event.addObject(objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS).convertValue(body, Map.class));
        event.setOperation(Operation.UPDATE);
        fintAuditService.audit(event);

        consumerEventUtil.send(event);

        statusCache.put(event.getCorrId(), event);

        URI location = UriComponentsBuilder.fromUriString(linker.self()).path("status/{id}").buildAndExpand(event.getCorrId()).toUri();
        return ResponseEntity.status(HttpStatus.ACCEPTED).location(location).build();
    }
  
    @PutMapping("/systemid/{id:.+}")
    public ResponseEntity putSakBySystemId(
            @PathVariable String id,
            @RequestHeader(name = HeaderConstants.ORG_ID) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT) String client,
            @RequestBody SakResource body
    ) {
        log.debug("putSakBySystemId {}, OrgId: {}, Client: {}", id, orgId, client);
        log.trace("Body: {}", body);
        linker.mapLinks(body);
        Event event = new Event(orgId, Constants.COMPONENT, NoarkActions.UPDATE_SAK, client);
        event.setQuery("systemid/" + id);
        event.addObject(objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS).convertValue(body, Map.class));
        event.setOperation(Operation.UPDATE);
        fintAuditService.audit(event);

        consumerEventUtil.send(event);

        statusCache.put(event.getCorrId(), event);

        URI location = UriComponentsBuilder.fromUriString(linker.self()).path("status/{id}").buildAndExpand(event.getCorrId()).toUri();
        return ResponseEntity.status(HttpStatus.ACCEPTED).location(location).build();
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

