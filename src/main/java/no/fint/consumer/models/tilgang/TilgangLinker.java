package no.fint.consumer.models.tilgang;

import no.fint.model.resource.arkiv.noark.TilgangResource;
import no.fint.model.resource.arkiv.noark.TilgangResources;
import no.fint.relations.FintLinker;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static org.springframework.util.StringUtils.isEmpty;

@Component
public class TilgangLinker extends FintLinker<TilgangResource> {

    public TilgangLinker() {
        super(TilgangResource.class);
    }

    public void mapLinks(TilgangResource resource) {
        super.mapLinks(resource);
    }

    @Override
    public TilgangResources toResources(Collection<TilgangResource> collection) {
        return toResources(collection.stream(), 0, 0, collection.size());
    }

    @Override
    public TilgangResources toResources(Stream<TilgangResource> stream, int offset, int size, int totalItems) {
        TilgangResources resources = new TilgangResources();
        stream.map(this::toResource).forEach(resources::addResource);
        addPagination(resources, offset, size, totalItems);
        return resources;
    }

    @Override
    public String getSelfHref(TilgangResource tilgang) {
        return getAllSelfHrefs(tilgang).findFirst().orElse(null);
    }

    @Override
    public Stream<String> getAllSelfHrefs(TilgangResource tilgang) {
        Stream.Builder<String> builder = Stream.builder();
        if (!isNull(tilgang.getSystemId()) && !isEmpty(tilgang.getSystemId().getIdentifikatorverdi())) {
            builder.add(createHrefWithId(tilgang.getSystemId().getIdentifikatorverdi(), "systemid"));
        }
        
        return builder.build();
    }

    int[] hashCodes(TilgangResource tilgang) {
        IntStream.Builder builder = IntStream.builder();
        if (!isNull(tilgang.getSystemId()) && !isEmpty(tilgang.getSystemId().getIdentifikatorverdi())) {
            builder.add(tilgang.getSystemId().getIdentifikatorverdi().hashCode());
        }
        
        return builder.build().toArray();
    }

}

