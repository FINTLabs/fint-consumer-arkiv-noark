package no.novari.fint.consumer.event

import no.fint.audit.FintAuditService
import no.fint.event.model.Event
import no.fint.event.model.Status
import no.fint.events.FintEvents
import spock.lang.Specification

import java.util.concurrent.BlockingQueue

class ConsumerEventUtilSpec extends Specification {
    private ConsumerEventUtil consumerEventUtil
    private FintEvents fintEvents
    private FintAuditService fintAuditService
    private SynchronousEvents synchronousEvents
    private BlockingQueue queue

    void setup() {
        fintEvents = Mock()
        fintAuditService = Mock()
        synchronousEvents = Mock()
        queue = Mock()
        consumerEventUtil = new ConsumerEventUtil(
                fintEvents: fintEvents,
                fintAuditService: fintAuditService,
                synchronousEvents: synchronousEvents
        )
    }

    def "Send and receive Event"() {
        given:
        def event = new Event(orgId: 'rogfk.no', corrId: '123')

        when:
        def response = consumerEventUtil.healthCheck(event)

        then:
        2 * fintAuditService.audit(_ as Event, _ as Status)
        1 * fintEvents.sendDownstream(event) >> true
        1 * synchronousEvents.register(event) >> queue
        1 * queue.poll(_, _) >> event
        response.isPresent()
    }
}
