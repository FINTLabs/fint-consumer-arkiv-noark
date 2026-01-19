package no.novari.fint.consumer.models.autorisasjon;

import no.novari.fint.model.resource.arkiv.noark.AutorisasjonResource;
import no.novari.fint.model.resource.arkiv.noark.AutorisasjonResources;
import no.novari.fint.relations.FintLinker;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static org.springframework.util.StringUtils.isEmpty;

@Component
public class AutorisasjonLinker extends FintLinker<AutorisasjonResource> {

    public AutorisasjonLinker() {
        super(AutorisasjonResource.class);
    }

    public void mapLinks(AutorisasjonResource resource) {
        super.mapLinks(resource);
    }

    @Override
    public AutorisasjonResources toResources(Collection<AutorisasjonResource> collection) {
        return toResources(collection.stream(), 0, 0, collection.size());
    }

    @Override
    public AutorisasjonResources toResources(Stream<AutorisasjonResource> stream, int offset, int size, int totalItems) {
        AutorisasjonResources resources = new AutorisasjonResources();
        stream.map(this::toResource).forEach(resources::addResource);
        addPagination(resources, offset, size, totalItems);
        return resources;
    }

    @Override
    public String getSelfHref(AutorisasjonResource autorisasjon) {
        return getAllSelfHrefs(autorisasjon).findFirst().orElse(null);
    }

    @Override
    public Stream<String> getAllSelfHrefs(AutorisasjonResource autorisasjon) {
        Stream.Builder<String> builder = Stream.builder();
        if (!isNull(autorisasjon.getSystemId()) && !isEmpty(autorisasjon.getSystemId().getIdentifikatorverdi())) {
            builder.add(createHrefWithId(autorisasjon.getSystemId().getIdentifikatorverdi(), "systemid"));
        }
        
        return builder.build();
    }

    int[] hashCodes(AutorisasjonResource autorisasjon) {
        IntStream.Builder builder = IntStream.builder();
        if (!isNull(autorisasjon.getSystemId()) && !isEmpty(autorisasjon.getSystemId().getIdentifikatorverdi())) {
            builder.add(autorisasjon.getSystemId().getIdentifikatorverdi().hashCode());
        }
        
        return builder.build().toArray();
    }

}

