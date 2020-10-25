package no.fint.consumer.status

import com.fasterxml.jackson.databind.ObjectMapper
import no.fint.audit.FintAuditService
import no.fint.consumer.config.ConsumerProps
import no.fint.consumer.test.TestObject
import no.fint.event.model.Event
import no.fint.event.model.ResponseStatus
import no.fint.relations.FintLinker
import org.springframework.http.HttpStatus
import spock.lang.Specification

class StatusCacheSpec extends Specification {
    StatusCache statusCache
    FintAuditService fintAuditService
    ConsumerProps consumerProps
    ObjectMapper objectMapper

    void setup() {
        fintAuditService = Stub()
        consumerProps = Stub(ConsumerProps) {
            isUseCreated() >> true
        }
        objectMapper = Mock()
        statusCache = new StatusCache(
                cacheSpec: 'expireAfterWrite=30m',
                fintAuditService: fintAuditService,
                props: consumerProps,
                objectMapper: objectMapper)
        statusCache.init()
    }

    def 'Produces valid responses'() {
        given:
        def linker = Mock(FintLinker)

        when:
        def result = statusCache.handleStatusRequest('missing', 'test.org', linker, TestObject)

        then:
        result.statusCode == HttpStatus.GONE

        when:
        statusCache.put('empty', new Event(orgId: 'test.org'))
        result = statusCache.handleStatusRequest('empty', 'test.org', linker, TestObject)

        then:
        result.statusCode == HttpStatus.ACCEPTED

        when:
        statusCache.put('abc', new Event(orgId: 'test.org', responseStatus: ResponseStatus.ACCEPTED, data: [null]))
        result = statusCache.handleStatusRequest('abc', 'test.org', linker, TestObject)

        then:
        result.statusCode == HttpStatus.CREATED
        1 * linker.getSelfHref(_) >> '/foo'
        1 * objectMapper.convertValue(_, TestObject)

        when:
        statusCache.put('def', new Event(orgId: 'test.org', responseStatus: ResponseStatus.REJECTED, statusCode: 'NOT_FOUND'))
        result = statusCache.handleStatusRequest('def', 'test.org', linker, TestObject)

        then:
        result.statusCode == HttpStatus.NOT_FOUND

        when:
        statusCache.put('ghi', new Event(orgId: 'test.org', responseStatus: ResponseStatus.CONFLICT, data: [null]))
        result = statusCache.handleStatusRequest('ghi', 'test.org', linker, TestObject)

        then:
        result.statusCode == HttpStatus.CONFLICT
        1 * objectMapper.convertValue(_, TestObject)
    }
}
