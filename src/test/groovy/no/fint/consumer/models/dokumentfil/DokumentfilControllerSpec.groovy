package no.fint.consumer.models.dokumentfil

import no.fint.consumer.event.ConsumerEventUtil
import no.fint.consumer.event.SynchronousEvents
import no.fint.event.model.Event
import no.fint.event.model.Operation
import no.fint.event.model.ResponseStatus
import no.fint.model.resource.arkiv.noark.DokumentfilResource
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.CoreMatchers.startsWith
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/*
 * This specification asserts that the custom mappings for Dokumentfil
 * are present.
 */

@SpringBootTest(properties = 'fint.consumer.cache.disabled.dokumentfil=true')
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DokumentfilControllerSpec extends Specification {
    @Autowired
    MockMvc mockMvc

    @SpringBean
    ConsumerEventUtil consumerEventUtil = Mock()

    @SpringBean
    SynchronousEvents synchronousEvents = Mock()

    def 'Assert that the proper GET mapping is present'() {
        given:
        def queue = Mock(BlockingQueue)
        def event = new Event(
                responseStatus: ResponseStatus.ACCEPTED,
                data: [
                        new DokumentfilResource(
                                format: 'application/pdf',
                                filnavn: 'testfil.pdf',
                                data: Base64.encoder.encodeToString(getClass().getResourceAsStream('/testfile.pdf').getBytes())
                        )
                ]
        )

        when:
        def response = mockMvc.perform(
                get('/dokumentfil/systemid/abcd1234')
                        .header('x-org-id', 'test.org')
                        .header('x-client', 'Spock'))

        then:
        response
                .andExpect(status().isOk())
                .andExpect(header().string('Content-Type', containsString('application/pdf')))
                .andExpect(content().string(startsWith('%PDF-1.4')))
        1 * synchronousEvents.register({ it.request.query == 'systemId/abcd1234' }) >> queue
        1 * queue.poll(5, TimeUnit.MINUTES) >> event
    }

    def 'Assert that the proper POST mapping is present'() {
        given:
        def content = getClass().getResourceAsStream('/testfile.pdf').getBytes()

        when:
        def response = mockMvc.perform(
                post('/dokumentfil')
                        .header('x-org-id', 'test.org')
                        .header('x-client', 'Spock')
                        .contentType(MediaType.APPLICATION_PDF)
                        .header('Content-Disposition', 'attachment; filename="testfile.pdf"')
                        .content(content))

        then:
        response.andExpect(status().isAccepted()).andExpect(header().string('Location', containsString('/dokumentfil/status/')))
        1 * consumerEventUtil.send({
            it.action == 'UPDATE_DOKUMENTFIL' && it.operation == Operation.CREATE && it.data.every { it =~ /testfile/ }
        })
    }

    def 'Assert that the proper PUT mapping is present'() {
        given:
        def content = getClass().getResourceAsStream('/testfile.pdf').getBytes()

        when:
        def response = mockMvc.perform(
                put('/dokumentfil/systemid/abcd1234')
                        .header('x-org-id', 'test.org')
                        .header('x-client', 'Spock')
                        .contentType(MediaType.APPLICATION_PDF)
                        .header('Content-Disposition', 'attachment; filename="testfile.pdf"')
                        .content(content))

        then:
        response.andExpect(status().isAccepted()).andExpect(header().string('Location', containsString('/dokumentfil/status/')))
        1 * consumerEventUtil.send({
            it.action == 'UPDATE_DOKUMENTFIL' && it.operation == Operation.UPDATE && it.data.every { it =~ /testfile/ }
        })
    }
}
