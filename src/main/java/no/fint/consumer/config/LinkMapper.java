package no.fint.consumer.config;

import no.fint.consumer.utils.RestEndpoints;
import java.util.Map;
import com.google.common.collect.ImmutableMap;
import no.novari.fint.model.arkiv.noark.AdministrativEnhet;
import no.novari.fint.model.arkiv.noark.Arkivdel;
import no.novari.fint.model.arkiv.noark.Arkivressurs;
import no.novari.fint.model.arkiv.noark.Autorisasjon;
import no.novari.fint.model.arkiv.noark.Dokumentfil;
import no.novari.fint.model.arkiv.noark.Klasse;
import no.novari.fint.model.arkiv.noark.Klassifikasjonssystem;
import no.novari.fint.model.arkiv.noark.Sak;
import no.novari.fint.model.arkiv.noark.Tilgang;

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
            .put("no.novari.fint.model.administrasjon.organisasjon.Organisasjonselement", "/administrasjon/organisasjon/organisasjonselement")
            .put("no.novari.fint.model.felles.kodeverk.iso.Landkode", "/felles/kodeverk/iso/landkode")
            .put("no.novari.fint.model.administrasjon.personal.Personalressurs", "/administrasjon/personal/personalressurs")
            .put("no.novari.fint.model.arkiv.kodeverk.Tilgangsrestriksjon", "/arkiv/kodeverk/tilgangsrestriksjon")
            .put("no.novari.fint.model.arkiv.kodeverk.DokumentStatus", "/arkiv/kodeverk/dokumentstatus")
            .put("no.novari.fint.model.arkiv.kodeverk.DokumentType", "/arkiv/kodeverk/dokumenttype")
            .put("no.novari.fint.model.arkiv.kodeverk.TilknyttetRegistreringSom", "/arkiv/kodeverk/tilknyttetregistreringsom")
            .put("no.novari.fint.model.arkiv.kodeverk.Format", "/arkiv/kodeverk/format")
            .put("no.novari.fint.model.arkiv.kodeverk.Variantformat", "/arkiv/kodeverk/variantformat")
            .put("no.novari.fint.model.arkiv.kodeverk.JournalpostType", "/arkiv/kodeverk/journalposttype")
            .put("no.novari.fint.model.arkiv.kodeverk.JournalStatus", "/arkiv/kodeverk/journalstatus")
            .put("no.novari.fint.model.arkiv.kodeverk.Tilgangsgruppe", "/arkiv/kodeverk/tilgangsgruppe")
            .put("no.novari.fint.model.arkiv.kodeverk.Klassifikasjonstype", "/arkiv/kodeverk/klassifikasjonstype")
            .put("no.novari.fint.model.arkiv.kodeverk.KorrespondansepartType", "/arkiv/kodeverk/korrespondanseparttype")
            .put("no.novari.fint.model.arkiv.kodeverk.Merknadstype", "/arkiv/kodeverk/merknadstype")
            .put("no.novari.fint.model.arkiv.kodeverk.PartRolle", "/arkiv/kodeverk/partrolle")
            .put("no.novari.fint.model.arkiv.kodeverk.Saksmappetype", "/arkiv/kodeverk/saksmappetype")
            .put("no.novari.fint.model.arkiv.kodeverk.Saksstatus", "/arkiv/kodeverk/saksstatus")
            .put("no.novari.fint.model.arkiv.kodeverk.Skjermingshjemmel", "/arkiv/kodeverk/skjermingshjemmel")
            .put("no.novari.fint.model.arkiv.kodeverk.Rolle", "/arkiv/kodeverk/rolle")
            /* .put(TODO,TODO) */
            .build();
    }

}
