package no.fint.consumer.models.arkivressurs

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import spock.lang.Specification

import static org.hamcrest.CoreMatchers.containsString
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ArkivressursCustomControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc


    def 'Assert that DELETE Arkivressurs by kildesystemid is present'() {
        given:
            def kildesystemid = 'ARKIVAR'
            def orgId = 'fintlabs.no'
            def client = 'test@fintlabs.no'

        when:
            def response = mockMvc.perform(
                    MockMvcRequestBuilders.delete("/arkivressurs/kildesystemid/{id}", kildesystemid)
                            .header('x-org-id', orgId)
                            .header('x-client', client)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
            );

        then:
            response
                .andExpect(status().isAccepted())
                .andExpect(header().string('Location', containsString('/arkivressurs/status/')))
    }

    def 'Assert that DELETE Arkivressurs by systemid is present'() {
        given:
            def systemid = 'ARKIVAR'
            def orgId = 'fintlabs.no'
            def client = 'test@fintlabs.no'

        when:
            def response = mockMvc.perform(
                    MockMvcRequestBuilders.delete("/arkivressurs/systemid/{id}", systemid)
                            .header('x-org-id', orgId)
                            .header('x-client', client)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
            );

        then:
            response
                    .andExpect(status().isAccepted())
                    .andExpect(header().string('Location', containsString('/arkivressurs/status/')))
    }
}
