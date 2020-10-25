package no.fint.consumer.event;

import lombok.extern.slf4j.Slf4j;
import no.fint.audit.FintAuditService;
import no.fint.event.model.Event;
import no.fint.event.model.Status;
import no.fint.event.model.health.Health;
import no.fint.events.FintEvents;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ConsumerEventUtil {

    @Autowired
    private FintEvents fintEvents;

    @Autowired
    private FintAuditService fintAuditService;

    @Autowired
    private SynchronousEvents synchronousEvents;

    public Optional<Event<Health>> healthCheck(Event<Health> event) {
        log.debug("Sending health check event {} to {}", event.getAction(), event.getOrgId());

        BlockingQueue<Event> queue = synchronousEvents.register(event);
        send(event);

        try {
            Event<Health> response = queue.poll(30, TimeUnit.SECONDS);
            if (response != null) {
                fintAuditService.audit(response, Status.SENT_TO_CLIENT);
                return Optional.of(response);
            } else {
                fintAuditService.audit(event, Status.NO_RESPONSE_FROM_ADAPTER, Status.SENT_TO_CLIENT);
                return Optional.empty();
            }
        } catch (InterruptedException e) {
            fintAuditService.audit(event, Status.ERROR);
            return Optional.empty();
        }
    }

    public void send(Event event) {
        fintAuditService.audit(event);
        fintAuditService.audit(event, Status.DOWNSTREAM_QUEUE);
        log.debug("Sending event {} to {}", event.getAction(), event.getOrgId());
        fintEvents.sendDownstream(event);
    }
}
