package no.fint.consumer.config;

import no.fint.consumer.utils.RestEndpoints;
import java.util.Map;
import com.google.common.collect.ImmutableMap;
import no.novari.fint.model.arkiv.noark.*;

public class LinkMapper {

    public static Map<String, String> linkMapper(String contextPath) {
        return ImmutableMap.<String,String>builder()
            .put(AdministrativEnhet.class.getName(), contextPath + RestEndpoints.ADMINISTRATIVENHET)
            .put(Arkivdel.class.getName(), contextPath + RestEndpoints.ARKIVDEL)
            .put(Arkivressurs.class.getName(), contextPath + RestEndpoints.ARKIVRESSURS)
            .put(Autorisasjon.class.getName(), contextPath + RestEndpoints.AUTORISASJON)
            .put(Dokumentfil.class.getName(), contextPath + RestEndpoints.DOKUMENTFIL)
            .put(Klasse.class.getName(), contextPath + RestEndpoints.KLASSE)
            .put(Klassifikasjonssystem.class.getName(), contextPath + RestEndpoints.KLASSIFIKASJONSSYSTEM)
            .put(Sak.class.getName(), contextPath + RestEndpoints.SAK)
            .put(Tilgang.class.getName(), contextPath + RestEndpoints.TILGANG)
            .put("no.novari.fint.model.administrasjon.organisasjon.Organisasjonselement", "/model/administrasjon/organisasjon/organisasjonselement")
            .put("no.novari.fint.model.felles.kodeverk.iso.Landkode", "/model/felles/kodeverk/iso/landkode")
            .put("no.novari.fint.model.administrasjon.personal.Personalressurs", "/model/administrasjon/personal/personalressurs")
            .put("no.novari.fint.model.arkiv.kodeverk.Tilgangsrestriksjon", "/model/arkiv/kodeverk/tilgangsrestriksjon")
            .put("no.novari.fint.model.arkiv.kodeverk.DokumentStatus", "/model/arkiv/kodeverk/dokumentstatus")
            .put("no.novari.fint.model.arkiv.kodeverk.DokumentType", "/model/arkiv/kodeverk/dokumenttype")
            .put("no.novari.fint.model.arkiv.kodeverk.TilknyttetRegistreringSom", "/model/arkiv/kodeverk/tilknyttetregistreringsom")
            .put("no.novari.fint.model.arkiv.kodeverk.Format", "/model/arkiv/kodeverk/format")
            .put("no.novari.fint.model.arkiv.kodeverk.Variantformat", "/model/arkiv/kodeverk/variantformat")
            .put("no.novari.fint.model.arkiv.kodeverk.JournalpostType", "/model/arkiv/kodeverk/journalposttype")
            .put("no.novari.fint.model.arkiv.kodeverk.JournalStatus", "/model/arkiv/kodeverk/journalstatus")
            .put("no.novari.fint.model.arkiv.kodeverk.Tilgangsgruppe", "/model/arkiv/kodeverk/tilgangsgruppe")
            .put("no.novari.fint.model.utdanning.kodeverk.Skolear", "/model/utdanning/kodeverk/skolear")
            .put("no.novari.fint.model.utdanning.kodeverk.Termin", "/model/utdanning/kodeverk/termin")
            .put("no.novari.fint.model.utdanning.utdanningsprogram.Arstrinn", "/model/utdanning/utdanningsprogram/arstrinn")
            .put("no.novari.fint.model.utdanning.utdanningsprogram.Skole", "/model/utdanning/utdanningsprogram/skole")
            .put("no.novari.fint.model.utdanning.elev.Undervisningsforhold", "/model/utdanning/elev/undervisningsforhold")
            .put("no.novari.fint.model.utdanning.elev.Klassemedlemskap", "/model/utdanning/elev/klassemedlemskap")
            .put("no.novari.fint.model.utdanning.elev.Kontaktlarergruppe", "/model/utdanning/elev/kontaktlarergruppe")
            .put("no.novari.fint.model.arkiv.kodeverk.Klassifikasjonstype", "/model/arkiv/kodeverk/klassifikasjonstype")
            .put("no.novari.fint.model.arkiv.kodeverk.KorrespondansepartType", "/model/arkiv/kodeverk/korrespondanseparttype")
            .put("no.novari.fint.model.arkiv.kodeverk.Merknadstype", "/model/arkiv/kodeverk/merknadstype")
            .put("no.novari.fint.model.arkiv.kodeverk.PartRolle", "/model/arkiv/kodeverk/partrolle")
            .put("no.novari.fint.model.arkiv.kodeverk.Saksmappetype", "/model/arkiv/kodeverk/saksmappetype")
            .put("no.novari.fint.model.arkiv.kodeverk.Saksstatus", "/model/arkiv/kodeverk/saksstatus")
            .put("no.novari.fint.model.arkiv.kodeverk.Skjermingshjemmel", "/model/arkiv/kodeverk/skjermingshjemmel")
            .put("no.novari.fint.model.arkiv.kodeverk.Rolle", "/model/arkiv/kodeverk/rolle")
            /* .put(TODO,TODO) */
            .build();
    }

}
