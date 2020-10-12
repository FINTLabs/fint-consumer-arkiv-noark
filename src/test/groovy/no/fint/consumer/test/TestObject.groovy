package no.fint.consumer.test

import no.fint.model.resource.FintLinks
import no.fint.model.resource.Link

class TestObject implements FintLinks {
    @Override
    Map<String, List<Link>> getLinks() {
        return []
    }
}
