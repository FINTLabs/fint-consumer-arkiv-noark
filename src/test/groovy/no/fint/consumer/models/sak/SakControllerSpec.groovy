package no.fint.consumer.models.sak

import com.fasterxml.jackson.databind.ObjectMapper
import no.fint.consumer.event.SynchronousEvents
import no.fint.event.model.Event
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

import static org.hamcrest.CoreMatchers.equalTo
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/*
 * This specification asserts that the custom mappings for /mappeid/{year}/{sequence}
 * are present.
 */

@SpringBootTest(properties = 'fint.consumer.cache.disabled.sak=true')
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SakControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    ObjectMapper objectMapper

    @SpringBean
    SynchronousEvents synchronousEvents = Mock()

    def "Verify that GET by mappeId works"() {
        given:
        def event = objectMapper.readValue('''{
    "corrId": "aaf3518e-c9b1-4152-a117-d536c166b0bb",
    "action": "GET_SAK",
    "operation": "READ",
    "status": "DOWNSTREAM_QUEUE",
    "time": 1586843296434,
    "orgId": "mock.no",
    "source": "kulturminnevern",
    "client": "CACHE_SERVICE",
    "data": [
        {
            "mappeId": {
                "identifikatorverdi": "2020/42"
            },
            "tittel": "Spock"
        }
    ],
    "responseStatus": "ACCEPTED",
    "query": "mappeId/2020/42"
}''', Event)
        def queue = Mock(BlockingQueue)
        when:
        def response = mockMvc.perform(
                get('/sak/mappeid/2020/42')
                        .header('x-org-id', 'test.org')
                        .header('x-client', 'Spock'))

        then:
        response.andExpect(status().is2xxSuccessful()).andExpect(jsonPath('$.tittel').value(equalTo('Spock')))
        1 * synchronousEvents.register({ it.request.query == 'mappeId/2020/42' }) >> queue
        1 * queue.poll(5, TimeUnit.MINUTES) >> event
    }

    def "Verify that OdataFilter works"() {
        given:
        def event = objectMapper.readValue('''{
    "corrId": "aaf3518e-c9b1-4152-a117-d536c166b0bb",
    "action": "GET_SAK",
    "operation": "READ",
    "status": "DOWNSTREAM_QUEUE",
    "time": 1586843296434,
    "orgId": "mock.no",
    "source": "kulturminnevern",
    "client": "CACHE_SERVICE",
    "data": [
        {
            "mappeId": {
                "identifikatorverdi": "2020/42"
            },
            "tittel": "Spock"
        }
    ],
    "responseStatus": "ACCEPTED",
    "query": "mappeId/2020/42"
}''', Event)
        def queue = Mock(BlockingQueue)
        when:
        def response = mockMvc.perform(
                get('/sak?$filter=systemId/identifikatorverdi eq \'system-id-1\'')
                        .header('x-org-id', 'test.org')
                        .header('x-client', 'Spock'))

        then:
        response.andExpect(status().is2xxSuccessful()).andExpect(jsonPath('$._embedded._entries[0].tittel').value(equalTo('Spock')))
        1 * synchronousEvents.register({ it.request.query == '$filter=systemId/identifikatorverdi eq \'system-id-1\''}) >> queue
        1 * queue.poll(5, TimeUnit.MINUTES) >> event
    }

}
