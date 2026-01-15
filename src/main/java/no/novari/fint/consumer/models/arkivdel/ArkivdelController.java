package no.novari.fint.consumer.models.arkivdel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import no.fint.audit.FintAuditService;

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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import no.novari.fint.model.resource.arkiv.noark.ArkivdelResource;
import no.novari.fint.model.resource.arkiv.noark.ArkivdelResources;
import no.novari.fint.model.arkiv.noark.NoarkActions;

@Slf4j
@Api(tags = {"Arkivdel"})
@CrossOrigin
@RestController
@RequestMapping(name = "Arkivdel", value = RestEndpoints.ARKIVDEL, produces = {FintRelationsMediaType.APPLICATION_HAL_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE})
public class ArkivdelController {

    private static final String ODATA_FILTER_QUERY_OPTION = "$filter=";

    @Autowired(required = false)
    private ArkivdelCacheService cacheService;

    @Autowired
    private FintAuditService fintAuditService;

    @Autowired
    private ArkivdelLinker linker;

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
            throw new CacheDisabledException("Arkivdel cache is disabled.");
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
            throw new CacheDisabledException("Arkivdel cache is disabled.");
        }
        if (props.isOverrideOrgId() || orgId == null) {
            orgId = props.getDefaultOrgId();
        }
        return ImmutableMap.of("size", cacheService.getCacheSize(orgId));
    }

    @GetMapping
    public ArkivdelResources getArkivdel(
            @RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT, required = false) String client,
            @RequestParam(defaultValue = "0") long sinceTimeStamp,
            @RequestParam(defaultValue = "0") int size,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String $filter,
            HttpServletRequest request) throws InterruptedException {
        if (cacheService == null) {
            if (StringUtils.isNotBlank($filter)) {
                return getArkivdelByOdataFilter(client, orgId, $filter);
            }
            throw new CacheDisabledException("Arkivdel cache is disabled.");
        }
        if (props.isOverrideOrgId() || orgId == null) {
            orgId = props.getDefaultOrgId();
        }
        if (client == null) {
            client = props.getDefaultClient();
        }
        log.debug("OrgId: {}, Client: {}", orgId, client);

        Event event = new Event(orgId, Constants.COMPONENT, NoarkActions.GET_ALL_ARKIVDEL, client);
        event.setOperation(Operation.READ);
        if (StringUtils.isNotBlank(request.getQueryString())) {
            event.setQuery("?" + request.getQueryString());
        }
        fintAuditService.audit(event);
        fintAuditService.audit(event, Status.CACHE);

        Stream<ArkivdelResource> resources;
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
    public ArkivdelResources getArkivdelByQuery(
            @RequestHeader(name = HeaderConstants.ORG_ID, required = false)   String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT, required = false) String client,
            @RequestParam(defaultValue = "0") long sinceTimeStamp,
            @RequestParam(defaultValue = "0") int  size,
            @RequestParam(defaultValue = "0") int  offset,
            @RequestBody(required = false) String query,
            HttpServletRequest request
    ) throws InterruptedException {
        return getArkivdel(orgId, client, sinceTimeStamp, size, offset, query, request);
    }

    private ArkivdelResources getArkivdelByOdataFilter(
        String client, String orgId, String $filter
    ) throws InterruptedException {
        if (!fintFilterService.validate($filter))
            throw new IllegalArgumentException("OData Filter is not valid");
    
        if (props.isOverrideOrgId() || orgId == null) orgId = props.getDefaultOrgId();
        if (client == null) client = props.getDefaultClient();
    
        Event event = new Event(
                orgId, Constants.COMPONENT,
                NoarkActions.GET_ARKIVDEL, client);
        event.setOperation(Operation.READ);
        event.setQuery(ODATA_FILTER_QUERY_OPTION.concat($filter));
    
        BlockingQueue<Event> queue = synchronousEvents.register(event);
        consumerEventUtil.send(event);
    
        Event response = EventResponses.handle(queue.poll(5, TimeUnit.MINUTES));
        if (response.getData() == null || response.getData().isEmpty())
            return new ArkivdelResources();
    
        ArrayList<ArkivdelResource> list = objectMapper.convertValue(
                response.getData(),
                new TypeReference<ArrayList<ArkivdelResource>>() {});
        fintAuditService.audit(response, Status.SENT_TO_CLIENT);
        list.forEach(r -> linker.mapAndResetLinks(r));
        return linker.toResources(list);
    }


    @GetMapping("/systemid/{id:.+}")
    public ArkivdelResource getArkivdelBySystemId(
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

        Event event = new Event(orgId, Constants.COMPONENT, NoarkActions.GET_ARKIVDEL, client);
        event.setOperation(Operation.READ);
        event.setQuery("systemId/" + id);

        if (cacheService != null) {
            fintAuditService.audit(event);
            fintAuditService.audit(event, Status.CACHE);

            Optional<ArkivdelResource> arkivdel = cacheService.getArkivdelBySystemId(orgId, id);

            fintAuditService.audit(event, Status.CACHE_RESPONSE, Status.SENT_TO_CLIENT);

            return arkivdel.map(linker::toResource).orElseThrow(() -> new EntityNotFoundException(id));

        } else {
            BlockingQueue<Event> queue = synchronousEvents.register(event);
            consumerEventUtil.send(event);

            Event response = EventResponses.handle(queue.poll(5, TimeUnit.MINUTES));

            if (response.getData() == null ||
                    response.getData().isEmpty()) throw new EntityNotFoundException(id);

            ArkivdelResource arkivdel = objectMapper.convertValue(response.getData().get(0), ArkivdelResource.class);

            fintAuditService.audit(response, Status.SENT_TO_CLIENT);

            return linker.mapAndResetLinks(arkivdel);
        }    
    }

}

