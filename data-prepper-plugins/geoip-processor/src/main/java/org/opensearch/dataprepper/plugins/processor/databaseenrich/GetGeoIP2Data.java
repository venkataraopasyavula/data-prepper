package org.opensearch.dataprepper.plugins.processor.databaseenrich;

import com.maxmind.db.Network;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.EnterpriseResponse;
import com.maxmind.geoip2.record.*;
import org.opensearch.dataprepper.plugins.processor.GeoIPProcessorConfig;
import org.opensearch.dataprepper.plugins.processor.databasedownload.DatabaseReaderCreate;
import org.opensearch.dataprepper.plugins.processor.databasedownload.DownloadSource;
import org.opensearch.dataprepper.plugins.processor.databasedownload.LicenseTypeOptions;
import org.opensearch.dataprepper.plugins.processor.loadtype.LoadTypeOptions;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class GetGeoIP2Data implements GetGeoData {

    private DatabaseReader.Builder readerEnterprise;
    private GeoIPProcessorConfig geoIPProcessorConfig;
    private DownloadSource downloadSource;
    private DatabaseReader.Builder readerCity;
    private DatabaseReader.Builder readerCountry;
    private DatabaseReader.Builder readerAsn;
    private LicenseTypeOptions licenseType;
    private Country country;
    private Continent continent;
    private City city;
    private Location location;
    private Subdivision subdivision;
    private Long asn;
    private String organization_name;
    private Network network;
    private GetGeoData geoData;
    private String dbPath;
    private LoadTypeOptions loadType;
    private int cacheSize;
    private Postal postal;
    private ZonedDateTime pluginStartDateTime;
    private ZonedDateTime pluginCurrentDateTime;

    public GetGeoIP2Data(String dbPath, int cacheSize, ZonedDateTime pluginStartDateTime, GeoIPProcessorConfig geoIPProcessorConfig) {
        this.dbPath = dbPath;
        this.cacheSize = cacheSize;
        this.pluginStartDateTime = pluginStartDateTime;
        pluginCurrentDateTime = LocalDateTime.now().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of(TimeZone.getTimeZone("UTC").getID()));
        this.geoIPProcessorConfig = geoIPProcessorConfig;
        this.loadType = geoIPProcessorConfig.getServiceType().getMaxMindService().getLoadType();
        initDatabaseReader();
    }

    public void initDatabaseReader() {
        readerEnterprise = DatabaseReaderCreate.createLoader(Path.of(dbPath + File.separator + GeoIP2EnterpriseDB), loadType, cacheSize);
    }

    @Override
    public void switchDatabaseReader() {
        closeReader();
        initDatabaseReader();
    }

    @Override
    public Map<String, Object> getGeoData(InetAddress inetAddress, List<String> attributes) {

        Map<String, Object> geoData = new HashMap<>();

        Duration diffDuration =  Duration.between(pluginStartDateTime, pluginCurrentDateTime);
        Duration refreshScheduleDuration = geoIPProcessorConfig.getServiceType().getMaxMindService().getCacheRefreshSchedule();
        if(diffDuration.getSeconds() >= refreshScheduleDuration.getSeconds()) {
            switchDatabaseReader();
        }

        try {

            EnterpriseResponse enterpriseResponse = readerEnterprise.build().enterprise(inetAddress);
            country = enterpriseResponse.getCountry();
            subdivision = enterpriseResponse.getMostSpecificSubdivision();
            city = enterpriseResponse.getCity();
            location = enterpriseResponse.getLocation();
            continent = enterpriseResponse.getContinent();
            postal = enterpriseResponse.getPostal();

        } catch (IOException ex) {
        } catch (GeoIp2Exception ex) {
        }

        if (attributes != null && attributes.isEmpty()) {

            for (String attribute : attributes) {
                switch (attribute) {
                    case "ip":
                        enrichData(geoData, "ip", inetAddress.toString());
                        break;
                    case "country_iso_code":
                        enrichData(geoData, "country_IsoCode", country.getIsoCode());
                        break;
                    case "country_name":
                        enrichData(geoData, "country_name", country.getName());
                        break;
                    case "continent_name":
                        enrichData(geoData, "continent_name", continent.getName());
                        break;
                    case "region_iso_code":
                        // ISO 3166-2 code for country subdivisions.
                        // See iso.org/iso-3166-country-codes.html
                        enrichRegionIsoCode(geoData, country.getIsoCode(),subdivision.getIsoCode());
                        break;
                    case "region_name":
                        enrichData(geoData, "region_name", subdivision.getName());
                        break;
                    case "city_name":
                        enrichData(geoData, "city_name", city.getName());
                        break;
                    case "timezoe":
                        enrichData(geoData, "timezone", location.getTimeZone());
                        break;
                    case "location":
                        enrichLocationData(geoData, location.getLatitude(),  location.getLongitude());
                        break;
                    case "postal":
                        enrichData(geoData, "postalCode", postal.getCode());
                        break;
                    case "asn":
                        if (asn != null) {
                            geoData.put("asn", asn);
                        }
                        break;
                    case "organization_name":
                        enrichData(geoData, "organization_name", organization_name);
                        break;
                    case "network":
                        enrichData(geoData, "network", network.toString());
                        break;
                }
            }
        } else {

            enrichData(geoData, "ip", inetAddress.toString());
            enrichData(geoData, "country_IsoCode", country.getIsoCode());
            enrichData(geoData, "country_name", country.getName());
            enrichData(geoData, "continent_name", continent.getName());

            enrichRegionIsoCode(geoData, country.getIsoCode(),subdivision.getIsoCode());

            enrichData(geoData, "region_name", subdivision.getName());
            enrichData(geoData, "city_name", city.getName());
            enrichData(geoData, "postalCode", postal.getCode());

            enrichData(geoData, "timezone", location.getTimeZone());
            enrichLocationData(geoData, location.getLatitude(),  location.getLongitude());

            if (asn != null) {
                geoData.put("asn", asn);
            }

            enrichData(geoData, "organization_name", organization_name);
            enrichData(geoData, "network", network.toString());
        }
        return geoData;
    }


    @Override
    public void closeReader() {
        try {
            if (readerEnterprise != null)
                readerEnterprise.build().close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
