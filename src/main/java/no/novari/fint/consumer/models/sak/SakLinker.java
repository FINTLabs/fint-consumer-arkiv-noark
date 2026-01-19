package no.novari.fint.consumer.models.sak;

import no.novari.fint.model.resource.arkiv.noark.SakResource;
import no.novari.fint.model.resource.arkiv.noark.SakResources;
import no.novari.fint.relations.FintLinker;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static org.springframework.util.StringUtils.isEmpty;

@Component
public class SakLinker extends FintLinker<SakResource> {

    public SakLinker() {
        super(SakResource.class);
    }

    public void mapLinks(SakResource resource) {
        super.mapLinks(resource);
    }

    @Override
    public SakResources toResources(Collection<SakResource> collection) {
        return toResources(collection.stream(), 0, 0, collection.size());
    }

    @Override
    public SakResources toResources(Stream<SakResource> stream, int offset, int size, int totalItems) {
        SakResources resources = new SakResources();
        stream.map(this::toResource).forEach(resources::addResource);
        addPagination(resources, offset, size, totalItems);
        return resources;
    }

    @Override
    public String getSelfHref(SakResource sak) {
        return getAllSelfHrefs(sak).findFirst().orElse(null);
    }

    @Override
    public Stream<String> getAllSelfHrefs(SakResource sak) {
        Stream.Builder<String> builder = Stream.builder();
        if (!isNull(sak.getMappeId()) && !isEmpty(sak.getMappeId().getIdentifikatorverdi())) {
            builder.add(createHrefWithId(sak.getMappeId().getIdentifikatorverdi(), "mappeid"));
        }
        if (!isNull(sak.getSystemId()) && !isEmpty(sak.getSystemId().getIdentifikatorverdi())) {
            builder.add(createHrefWithId(sak.getSystemId().getIdentifikatorverdi(), "systemid"));
        }
        
        return builder.build();
    }

    int[] hashCodes(SakResource sak) {
        IntStream.Builder builder = IntStream.builder();
        if (!isNull(sak.getMappeId()) && !isEmpty(sak.getMappeId().getIdentifikatorverdi())) {
            builder.add(sak.getMappeId().getIdentifikatorverdi().hashCode());
        }
        if (!isNull(sak.getSystemId()) && !isEmpty(sak.getSystemId().getIdentifikatorverdi())) {
            builder.add(sak.getSystemId().getIdentifikatorverdi().hashCode());
        }
        
        return builder.build().toArray();
    }

}

