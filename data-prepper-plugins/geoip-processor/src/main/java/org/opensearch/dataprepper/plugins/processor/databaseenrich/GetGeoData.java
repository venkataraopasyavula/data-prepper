package org.opensearch.dataprepper.plugins.processor.databaseenrich;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface GetGeoData {

    public final String GeoLite2CityDB = "GeoLite2-City.mmdb";
    public final String GeoLite2CountryDB = "GeoLite2-Country.mmdb";
    public final String GeoLite2AsnDB = "GeoLite2-ASN.mmdb";
    public final String GeoIP2EnterpriseDB = "GeoIP2-Enterprise.mmdb";
    public void switchDatabaseReader();
    public void closeReader();
    public Map<String, Object> getGeoData(InetAddress inetAddress, List<String> attributes);

    default public void enrichData(Map<String, Object> geoData,String attributeName, String attributeValue) {
        if (attributeValue != null) {
            geoData.put(attributeName, attributeValue);
        }
    }

   default public void enrichRegionIsoCode(Map<String, Object> geoData, String countryIso, String subdivisionIso) {
        if (countryIso != null && subdivisionIso != null) {
            enrichData(geoData, "region_iso_code", countryIso + "-" + subdivisionIso);
        }
    }

    default public void enrichLocationData(Map<String, Object> geoData, Double latitude, Double longitude) {

        if (latitude != null && longitude != null) {
            Map<String, Object> locationObject = new HashMap<>();
            locationObject.put("lat", latitude);
            locationObject.put("lon", longitude);
            geoData.put("location", locationObject);
        }
    }
}
