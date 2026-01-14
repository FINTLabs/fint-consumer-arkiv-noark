package no.novari.fint.consumer.event

import no.fint.audit.FintAuditService
import no.novari.fint.consumer.status.StatusCache
import no.fint.event.model.Event
import no.fint.event.model.Status
import spock.lang.Specification

class EventListenerSpec extends Specification {
    private EventListener eventListener
    private StatusCache statusCache
    private FintAuditService fintAuditService
    private SynchronousEvents synchronousEvents

    void setup() {
        fintAuditService = Mock()
        statusCache = Mock()
        synchronousEvents = Mock()
        eventListener = new EventListener(
                cacheServices: [],
                statusCache: statusCache,
                synchronousEvents: synchronousEvents,
                fintAuditService: fintAuditService)
    }

    def "No exception is thrown when receiving event"() {
        when:
        eventListener.accept(new Event(corrId: '123'))

        then:
        noExceptionThrown()
        1 * statusCache.containsKey(_ as String) >> false
        1 * fintAuditService.audit(_ as Event, _ as Status)
    }
}
