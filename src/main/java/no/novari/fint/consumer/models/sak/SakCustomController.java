package no.novari.fint.consumer.models.sak;

import no.novari.fint.consumer.utils.RestEndpoints;
import no.fint.event.model.HeaderConstants;
import no.novari.fint.model.resource.arkiv.noark.SakResource;
import no.novari.fint.relations.FintRelationsMediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping(path = RestEndpoints.SAK, produces = {FintRelationsMediaType.APPLICATION_HAL_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE})
public class SakCustomController {
    @Autowired
    private SakController sakController;

    @GetMapping("/mappeid/{ar}/{sekvensnummer}")
    public SakResource getSakByMappeArSekvensnummer(
            @PathVariable String ar,
            @PathVariable String sekvensnummer,
            @RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT, required = false) String client) throws InterruptedException {
        return sakController.getSakByMappeId(ar + "/" + sekvensnummer, orgId, client);
    }

    @PutMapping("/mappeid/{ar}/{sekvensnummer}")
    public ResponseEntity putSakByMappeArSekvensnummer(
            @PathVariable String ar,
            @PathVariable String sekvensnummer,
            @RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT, required = false) String client,
            @RequestBody SakResource body) throws InterruptedException {
        return sakController.putSakByMappeId(ar + "/" + sekvensnummer, orgId, client, body);
    }

}
