package no.novari.fint.consumer.models.administrativenhet;

import no.novari.fint.model.resource.arkiv.noark.AdministrativEnhetResource;
import no.novari.fint.model.resource.arkiv.noark.AdministrativEnhetResources;
import no.novari.fint.relations.FintLinker;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static org.springframework.util.StringUtils.isEmpty;

@Component
public class AdministrativEnhetLinker extends FintLinker<AdministrativEnhetResource> {

    public AdministrativEnhetLinker() {
        super(AdministrativEnhetResource.class);
    }

    public void mapLinks(AdministrativEnhetResource resource) {
        super.mapLinks(resource);
    }

    @Override
    public AdministrativEnhetResources toResources(Collection<AdministrativEnhetResource> collection) {
        return toResources(collection.stream(), 0, 0, collection.size());
    }

    @Override
    public AdministrativEnhetResources toResources(Stream<AdministrativEnhetResource> stream, int offset, int size, int totalItems) {
        AdministrativEnhetResources resources = new AdministrativEnhetResources();
        stream.map(this::toResource).forEach(resources::addResource);
        addPagination(resources, offset, size, totalItems);
        return resources;
    }

    @Override
    public String getSelfHref(AdministrativEnhetResource administrativenhet) {
        return getAllSelfHrefs(administrativenhet).findFirst().orElse(null);
    }

    @Override
    public Stream<String> getAllSelfHrefs(AdministrativEnhetResource administrativenhet) {
        Stream.Builder<String> builder = Stream.builder();
        if (!isNull(administrativenhet.getSystemId()) && !isEmpty(administrativenhet.getSystemId().getIdentifikatorverdi())) {
            builder.add(createHrefWithId(administrativenhet.getSystemId().getIdentifikatorverdi(), "systemid"));
        }
        
        return builder.build();
    }

    int[] hashCodes(AdministrativEnhetResource administrativenhet) {
        IntStream.Builder builder = IntStream.builder();
        if (!isNull(administrativenhet.getSystemId()) && !isEmpty(administrativenhet.getSystemId().getIdentifikatorverdi())) {
            builder.add(administrativenhet.getSystemId().getIdentifikatorverdi().hashCode());
        }
        
        return builder.build().toArray();
    }

}

