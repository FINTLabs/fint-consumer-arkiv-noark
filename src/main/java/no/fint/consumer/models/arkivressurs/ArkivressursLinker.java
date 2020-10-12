package no.fint.consumer.models.arkivressurs;

import no.fint.model.resource.arkiv.noark.ArkivressursResource;
import no.fint.model.resource.arkiv.noark.ArkivressursResources;
import no.fint.relations.FintLinker;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static org.springframework.util.StringUtils.isEmpty;

@Component
public class ArkivressursLinker extends FintLinker<ArkivressursResource> {

    public ArkivressursLinker() {
        super(ArkivressursResource.class);
    }

    public void mapLinks(ArkivressursResource resource) {
        super.mapLinks(resource);
    }

    @Override
    public ArkivressursResources toResources(Collection<ArkivressursResource> collection) {
        return toResources(collection.stream(), 0, 0, collection.size());
    }

    @Override
    public ArkivressursResources toResources(Stream<ArkivressursResource> stream, int offset, int size, int totalItems) {
        ArkivressursResources resources = new ArkivressursResources();
        stream.map(this::toResource).forEach(resources::addResource);
        addPagination(resources, offset, size, totalItems);
        return resources;
    }

    @Override
    public String getSelfHref(ArkivressursResource arkivressurs) {
        return getAllSelfHrefs(arkivressurs).findFirst().orElse(null);
    }

    @Override
    public Stream<String> getAllSelfHrefs(ArkivressursResource arkivressurs) {
        Stream.Builder<String> builder = Stream.builder();
        if (!isNull(arkivressurs.getKildesystemId()) && !isEmpty(arkivressurs.getKildesystemId().getIdentifikatorverdi())) {
            builder.add(createHrefWithId(arkivressurs.getKildesystemId().getIdentifikatorverdi(), "kildesystemid"));
        }
        if (!isNull(arkivressurs.getSystemId()) && !isEmpty(arkivressurs.getSystemId().getIdentifikatorverdi())) {
            builder.add(createHrefWithId(arkivressurs.getSystemId().getIdentifikatorverdi(), "systemid"));
        }
        
        return builder.build();
    }

    int[] hashCodes(ArkivressursResource arkivressurs) {
        IntStream.Builder builder = IntStream.builder();
        if (!isNull(arkivressurs.getKildesystemId()) && !isEmpty(arkivressurs.getKildesystemId().getIdentifikatorverdi())) {
            builder.add(arkivressurs.getKildesystemId().getIdentifikatorverdi().hashCode());
        }
        if (!isNull(arkivressurs.getSystemId()) && !isEmpty(arkivressurs.getSystemId().getIdentifikatorverdi())) {
            builder.add(arkivressurs.getSystemId().getIdentifikatorverdi().hashCode());
        }
        
        return builder.build().toArray();
    }

}

