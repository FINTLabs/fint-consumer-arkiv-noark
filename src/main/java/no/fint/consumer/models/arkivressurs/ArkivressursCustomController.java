package no.fint.consumer.models.arkivressurs;

import lombok.extern.slf4j.Slf4j;
import no.fint.audit.FintAuditService;
import no.fint.consumer.config.Constants;
import no.fint.consumer.event.ConsumerEventUtil;
import no.fint.consumer.status.StatusCache;
import no.fint.consumer.utils.RestEndpoints;
import no.fint.event.model.Event;
import no.fint.event.model.HeaderConstants;
import no.fint.event.model.Operation;
import no.fint.relations.FintRelationsMediaType;
import no.novari.fint.model.arkiv.noark.NoarkActions;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(name = "Arkivressurs", value = RestEndpoints.ARKIVRESSURS, produces = {FintRelationsMediaType.APPLICATION_HAL_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE})
public class ArkivressursCustomController {

    private final FintAuditService fintAuditService;
    private final ArkivressursLinker linker;
    private final StatusCache statusCache;
    private final ConsumerEventUtil consumerEventUtil;

    public ArkivressursCustomController(FintAuditService fintAuditService, ArkivressursLinker linker,
                                        StatusCache statusCache, ConsumerEventUtil consumerEventUtil) {
        this.fintAuditService = fintAuditService;
        this.linker = linker;
        this.statusCache = statusCache;
        this.consumerEventUtil = consumerEventUtil;
    }

    @DeleteMapping("/kildesystemid/{id:.+}")
    public ResponseEntity deleteArkivressursByKildesystemId(@PathVariable String id,
                                                            @RequestHeader(name = HeaderConstants.ORG_ID) String orgId,
                                                            @RequestHeader(name = HeaderConstants.CLIENT) String client) {

        log.info("Deleting arkivressurs by kildesystemId '{}' from {}, using client '{}'. Thanks for the ride, dude!",
                id, orgId, client);

        Event event = new Event(orgId, Constants.COMPONENT, NoarkActions.UPDATE_ARKIVRESSURS, client);
        event.setQuery("kildesystemid/" + id);
        event.setOperation(Operation.DELETE);

        fintAuditService.audit(event);
        consumerEventUtil.send(event);
        statusCache.put(event.getCorrId(), event);

        URI location = UriComponentsBuilder.fromUriString(linker.self()).path("status/{id}").buildAndExpand(event.getCorrId()).toUri();
        return ResponseEntity.status(HttpStatus.ACCEPTED).location(location).build();
    }

    @DeleteMapping("/systemid/{id:.+}")
    public ResponseEntity deleteArkivressursBySystemId(@PathVariable String id,
                                                       @RequestHeader(name = HeaderConstants.ORG_ID) String orgId,
                                                       @RequestHeader(name = HeaderConstants.CLIENT) String client) {

        log.info("Deleting arkivressurs by systemId '{}' from {}, using client '{}'. Thanks for the ride, dude!",
                id, orgId, client);

        Event event = new Event(orgId, Constants.COMPONENT, NoarkActions.UPDATE_ARKIVRESSURS, client);
        event.setQuery("systemid/" + id);
        event.setOperation(Operation.DELETE);

        fintAuditService.audit(event);
        consumerEventUtil.send(event);
        statusCache.put(event.getCorrId(), event);

        URI location = UriComponentsBuilder.fromUriString(linker.self()).path("status/{id}").buildAndExpand(event.getCorrId()).toUri();
        return ResponseEntity.status(HttpStatus.ACCEPTED).location(location).build();
    }
}