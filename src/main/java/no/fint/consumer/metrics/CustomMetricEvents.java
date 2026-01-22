package no.fint.consumer.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import no.fint.event.model.Event;
import no.fint.event.model.Operation;
import no.fint.event.model.ResponseStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "fint.consumer.custom.metric.events", havingValue = "true")
public class CustomMetricEvents {
    private static final String CUSTOM_METRIC_EVENTS = "fint.core.events";

    @Autowired
    private MeterRegistry meterRegistry;

    public void update(Event event) {
        meterRegistry.counter(CUSTOM_METRIC_EVENTS, getTags(event)).increment();
    }

    private List<Tag> getTags(Event event) {
        return Arrays.asList(
                Tag.of("orgId", event.getOrgId()),
                Tag.of("eventType", event.getAction()),
                Tag.of("eventOperation", Optional.ofNullable(event.getOperation()).map(Operation::name).orElse("READ")),
                Tag.of("eventResponseStatus", Optional.ofNullable(event.getResponseStatus()).map(ResponseStatus::name).orElse("NULL")));
    }
}