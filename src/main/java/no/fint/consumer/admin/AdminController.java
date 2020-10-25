package no.fint.consumer.admin;

import lombok.extern.slf4j.Slf4j;
import no.fint.cache.Cache;
import no.fint.cache.CacheManager;
import no.fint.cache.CacheService;
import no.fint.cache.utils.CacheUri;
import no.fint.consumer.config.Constants;
import no.fint.consumer.config.ConsumerProps;
import no.fint.consumer.event.ConsumerEventUtil;
import no.fint.consumer.utils.RestEndpoints;
import no.fint.event.model.DefaultActions;
import no.fint.event.model.Event;
import no.fint.event.model.HeaderConstants;
import no.fint.event.model.health.Health;
import no.fint.event.model.health.HealthStatus;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RestController
@CrossOrigin
@RequestMapping(value = RestEndpoints.ADMIN, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class AdminController {

    @Autowired
    private ConsumerEventUtil consumerEventUtil;

    @Autowired
    private CacheManager<?> cacheManager;

    @Autowired(required = false)
    private Collection<CacheService<?>> cacheServices;

    @Autowired
    private ConsumerProps props;

    @GetMapping("/health")
    public ResponseEntity<Event<Health>> healthCheck(@RequestHeader(HeaderConstants.ORG_ID) String orgId,
                                      @RequestHeader(HeaderConstants.CLIENT) String client) {
        log.debug("Health check on {} requested by {} ...", orgId, client);
        Event<Health> event = new Event<>(orgId, Constants.COMPONENT, DefaultActions.HEALTH, client);
        event.addData(new Health(Constants.COMPONENT_CONSUMER, HealthStatus.SENT_FROM_CONSUMER_TO_PROVIDER));

        final Optional<Event<Health>> response = consumerEventUtil.healthCheck(event);

        return response.map(health ->  {
            log.debug("Health check response: {}", health.getData());
            health.addData(new Health(Constants.COMPONENT_CONSUMER, HealthStatus.RECEIVED_IN_CONSUMER_FROM_PROVIDER));
            return ResponseEntity.ok(health);
        }).orElseGet(() -> {
            log.debug("No response to health event.");
            event.setMessage("No response from adapter");
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(event);
        });
    }

    @GetMapping("/organisations")
    public Collection<String> getOrganisations() {
        return cacheManager.getKeys();
    }

    @GetMapping("/organisations/{orgId:.+}")
    public Collection<String> getOrganization(@PathVariable String orgId) {
        return cacheManager.getKeys().stream().filter(key -> CacheUri.containsOrgId(key, orgId)).collect(Collectors.toList());
    }

    @GetMapping("/assets")
    public Collection<String> getAssets() {
        return props.getAssets();
    }

    @GetMapping("/caches")
    public Map<String, Integer> getCaches() {
        return cacheManager
                .getKeys()
                .stream()
                .collect(Collectors
                        .toMap(Function.identity(),
                                k -> cacheManager.getCache(k).map(Cache::size).orElse(0)));
    }

    @PostMapping({"/cache/rebuild", "/cache/rebuild/{model}"})
    public void rebuildCache(
            @RequestHeader(name = HeaderConstants.ORG_ID) String orgid,
            @RequestHeader(name = HeaderConstants.CLIENT) String client,
            @PathVariable(required = false) String model
    ) {
        log.info("Cache rebuild on {} requested by {}", orgid, client);
        cacheServices.stream()
                .filter(cacheService -> StringUtils.isBlank(model) || StringUtils.equalsIgnoreCase(cacheService.getModel(), model))
                .forEach(cacheService -> cacheService.populateCache(orgid));
    }

}