package no.fint.consumer.models.arkivdel;

import no.novari.fint.model.resource.arkiv.noark.ArkivdelResource;
import no.novari.fint.model.resource.arkiv.noark.ArkivdelResources;
import no.fint.relations.FintLinker;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static org.springframework.util.StringUtils.isEmpty;

@Component
public class ArkivdelLinker extends FintLinker<ArkivdelResource> {

    public ArkivdelLinker() {
        super(ArkivdelResource.class);
    }

    public void mapLinks(ArkivdelResource resource) {
        super.mapLinks(resource);
    }

    @Override
    public ArkivdelResources toResources(Collection<ArkivdelResource> collection) {
        return toResources(collection.stream(), 0, 0, collection.size());
    }

    @Override
    public ArkivdelResources toResources(Stream<ArkivdelResource> stream, int offset, int size, int totalItems) {
        ArkivdelResources resources = new ArkivdelResources();
        stream.map(this::toResource).forEach(resources::addResource);
        addPagination(resources, offset, size, totalItems);
        return resources;
    }

    @Override
    public String getSelfHref(ArkivdelResource arkivdel) {
        return getAllSelfHrefs(arkivdel).findFirst().orElse(null);
    }

    @Override
    public Stream<String> getAllSelfHrefs(ArkivdelResource arkivdel) {
        Stream.Builder<String> builder = Stream.builder();
        if (!isNull(arkivdel.getSystemId()) && !isEmpty(arkivdel.getSystemId().getIdentifikatorverdi())) {
            builder.add(createHrefWithId(arkivdel.getSystemId().getIdentifikatorverdi(), "systemid"));
        }
        
        return builder.build();
    }

    int[] hashCodes(ArkivdelResource arkivdel) {
        IntStream.Builder builder = IntStream.builder();
        if (!isNull(arkivdel.getSystemId()) && !isEmpty(arkivdel.getSystemId().getIdentifikatorverdi())) {
            builder.add(arkivdel.getSystemId().getIdentifikatorverdi().hashCode());
        }
        
        return builder.build().toArray();
    }

}

