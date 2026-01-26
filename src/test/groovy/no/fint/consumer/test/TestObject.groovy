package no.fint.consumer.test

import no.novari.fint.model.resource.FintLinks
import no.novari.fint.model.resource.Link

class TestObject implements FintLinks {
    @Override
    Map<String, List<Link>> getLinks() {
        return []
    }
}
