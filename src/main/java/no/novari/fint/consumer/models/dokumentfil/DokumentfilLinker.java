package no.novari.fint.consumer.models.dokumentfil;

import no.novari.fint.model.resource.arkiv.noark.DokumentfilResource;
import no.novari.fint.model.resource.arkiv.noark.DokumentfilResources;
import no.novari.fint.relations.FintLinker;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static org.springframework.util.StringUtils.isEmpty;

@Component
public class DokumentfilLinker extends FintLinker<DokumentfilResource> {

    public DokumentfilLinker() {
        super(DokumentfilResource.class);
    }

    public void mapLinks(DokumentfilResource resource) {
        super.mapLinks(resource);
    }

    @Override
    public DokumentfilResources toResources(Collection<DokumentfilResource> collection) {
        return toResources(collection.stream(), 0, 0, collection.size());
    }

    @Override
    public DokumentfilResources toResources(Stream<DokumentfilResource> stream, int offset, int size, int totalItems) {
        DokumentfilResources resources = new DokumentfilResources();
        stream.map(this::toResource).forEach(resources::addResource);
        addPagination(resources, offset, size, totalItems);
        return resources;
    }

    @Override
    public String getSelfHref(DokumentfilResource dokumentfil) {
        return getAllSelfHrefs(dokumentfil).findFirst().orElse(null);
    }

    @Override
    public Stream<String> getAllSelfHrefs(DokumentfilResource dokumentfil) {
        Stream.Builder<String> builder = Stream.builder();
        if (!isNull(dokumentfil.getSystemId()) && !isEmpty(dokumentfil.getSystemId().getIdentifikatorverdi())) {
            builder.add(createHrefWithId(dokumentfil.getSystemId().getIdentifikatorverdi(), "systemid"));
        }
        
        return builder.build();
    }

    int[] hashCodes(DokumentfilResource dokumentfil) {
        IntStream.Builder builder = IntStream.builder();
        if (!isNull(dokumentfil.getSystemId()) && !isEmpty(dokumentfil.getSystemId().getIdentifikatorverdi())) {
            builder.add(dokumentfil.getSystemId().getIdentifikatorverdi().hashCode());
        }
        
        return builder.build().toArray();
    }

}

