/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.plugins.processor.databaseenrich;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.dataprepper.plugins.processor.extension.GeoIPProcessorService;
import org.opensearch.dataprepper.plugins.processor.extension.databasedownload.DBSource;
import org.opensearch.dataprepper.plugins.processor.extension.GeoIpServiceConfig;
import org.opensearch.dataprepper.plugins.processor.extension.MaxMindConfig;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetGeoLite2DataTest {

    private static final String PATH = "./src/test/resources/mmdb-file/geo-lite2";
    public static final String IP = "2a02:ec00:0:0:0:0:0:0";
    private static final String PREFIX_DIR = "first_database";
    private String tempFolderPath = System.getProperty("java.io.tmpdir") + File.separator + "GeoIP";
    @Mock
    private GeoIpServiceConfig geoIpServiceConfig;
    @Mock
    private MaxMindConfig maxMindConfig;
    private GetGeoLite2Data getGeoLite2Data;
    private final int cacheSize = 4068;

    @BeforeEach
    void setUp() {
        when(geoIpServiceConfig.getMaxMindConfig()).thenReturn(maxMindConfig);
        when(maxMindConfig.getCacheSize()).thenReturn(cacheSize);

        final String dbPath = "./src/test/resources/mmdb-file/geo-lite2";
        getGeoLite2Data = new GetGeoLite2Data(dbPath, cacheSize);
    }

    @Test
    @Disabled
    void getGeoDataTest_without_attributes() throws UnknownHostException {
        List<String> attributes = List.of();
        InetAddress inetAddress = InetAddress.getByName(IP);
        GeoIPProcessorService.downloadReady = false;
        Map<String, Object> geoData = getGeoLite2Data.getGeoData(inetAddress, attributes, tempFolderPath);
        Assertions.assertNotNull(geoData);
        assertThat(geoData.get("country_iso_code"), equalTo("FR"));
        assertThat(geoData.get("ip"), equalTo(IP));
        assertDoesNotThrow(() -> {
            getGeoLite2Data.closeReader();
        });
    }

    @Test
    @Disabled
    void getGeoDataTest_with_attributes() throws UnknownHostException {
        List<String> attributes = List.of("city_name",
                "country_name",
                "ip",
                "country_iso_code",
                "continent_name",
                "region_iso_code",
                "region_name",
                "timezone",
                "location",
                "asn",
                "organization_name", "network");
        InetAddress inetAddress = InetAddress.getByName(IP);
        GeoIPProcessorService.downloadReady = false;
        Map<String, Object> geoData = getGeoLite2Data.getGeoData(inetAddress, attributes, tempFolderPath);
        Assertions.assertNotNull(geoData);
        assertThat(geoData.get("country_name"), equalTo("France"));
        assertThat(geoData.get("ip"), equalTo(IP));
        assertDoesNotThrow(() -> {
            getGeoLite2Data.closeReader();
        });
    }

    @Test
    @Disabled
    void getGeoDataTest_cover_EnrichFailedException() throws UnknownHostException {
        List<String> attributes = List.of();
        InetAddress inetAddress = InetAddress.getByName(IP);
        String dbPath = "/src/test/resources/mmdb-file/geo-enterprise";
        getGeoLite2Data = new GetGeoLite2Data(dbPath, cacheSize);
        GeoIPProcessorService.downloadReady = false;
        assertThrows(EnrichFailedException.class, () -> getGeoLite2Data.getGeoData(inetAddress, attributes, tempFolderPath));
    }

    @Test
    @Disabled
    void switchDatabaseReaderTest() {
        tempFolderPath = System.getProperty("java.io.tmpdir") + File.separator + "GeoIPMaxmind";
        DBSource.createFolderIfNotExist(tempFolderPath);
        getGeoLite2Data = new GetGeoLite2Data(tempFolderPath, cacheSize);
        assertDoesNotThrow(() -> {
            getGeoLite2Data.switchDatabaseReader();
        });
        assertDoesNotThrow(() -> {
            getGeoLite2Data.closeReader();
        });
    }

    @Test
    @Disabled
    void closeReaderTest() throws UnknownHostException {
        // While closing the readers, all the readers reset to null.
        getGeoLite2Data.closeReader();
        List<String> attributes = List.of("city_name", "country_name");
        InetAddress inetAddress = InetAddress.getByName(IP);
        assertThrows(NullPointerException.class, () -> getGeoLite2Data.getGeoData(inetAddress, attributes, PREFIX_DIR));
    }
}