package no.fint.consumer.config;

public enum Constants {
;

    public static final String COMPONENT = "arkiv-noark";
    public static final String COMPONENT_CONSUMER = COMPONENT + " consumer";
    public static final String CACHE_SERVICE = "CACHE_SERVICE";

    
    public static final String CACHE_INITIALDELAY_ADMINISTRATIVENHET = "${fint.consumer.cache.initialDelay.administrativenhet:900000}";
    public static final String CACHE_FIXEDRATE_ADMINISTRATIVENHET = "${fint.consumer.cache.fixedRate.administrativenhet:900000}";
    
    public static final String CACHE_INITIALDELAY_ARKIVDEL = "${fint.consumer.cache.initialDelay.arkivdel:1000000}";
    public static final String CACHE_FIXEDRATE_ARKIVDEL = "${fint.consumer.cache.fixedRate.arkivdel:900000}";
    
    public static final String CACHE_INITIALDELAY_ARKIVRESSURS = "${fint.consumer.cache.initialDelay.arkivressurs:1100000}";
    public static final String CACHE_FIXEDRATE_ARKIVRESSURS = "${fint.consumer.cache.fixedRate.arkivressurs:900000}";
    
    public static final String CACHE_INITIALDELAY_AUTORISASJON = "${fint.consumer.cache.initialDelay.autorisasjon:1200000}";
    public static final String CACHE_FIXEDRATE_AUTORISASJON = "${fint.consumer.cache.fixedRate.autorisasjon:900000}";
    
    public static final String CACHE_INITIALDELAY_DOKUMENTFIL = "${fint.consumer.cache.initialDelay.dokumentfil:1300000}";
    public static final String CACHE_FIXEDRATE_DOKUMENTFIL = "${fint.consumer.cache.fixedRate.dokumentfil:900000}";
    
    public static final String CACHE_INITIALDELAY_KLASSIFIKASJONSSYSTEM = "${fint.consumer.cache.initialDelay.klassifikasjonssystem:1400000}";
    public static final String CACHE_FIXEDRATE_KLASSIFIKASJONSSYSTEM = "${fint.consumer.cache.fixedRate.klassifikasjonssystem:900000}";
    
    public static final String CACHE_INITIALDELAY_SAK = "${fint.consumer.cache.initialDelay.sak:1500000}";
    public static final String CACHE_FIXEDRATE_SAK = "${fint.consumer.cache.fixedRate.sak:900000}";
    
    public static final String CACHE_INITIALDELAY_TILGANG = "${fint.consumer.cache.initialDelay.tilgang:1600000}";
    public static final String CACHE_FIXEDRATE_TILGANG = "${fint.consumer.cache.fixedRate.tilgang:900000}";
    

}
