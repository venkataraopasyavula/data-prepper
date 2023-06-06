package org.opensearch.dataprepper.plugins.processor;

import org.opensearch.dataprepper.plugins.processor.configuration.DatabasePathURLConfig;
import org.opensearch.dataprepper.plugins.processor.databasedownload.*;
import org.opensearch.dataprepper.plugins.processor.databaseenrich.GetGeoData;
import org.opensearch.dataprepper.plugins.processor.databaseenrich.GetGeoIP2Data;
import org.opensearch.dataprepper.plugins.processor.databaseenrich.GetGeoLite2Data;
import org.opensearch.dataprepper.plugins.processor.utils.DbSourceIdentification;
import org.opensearch.dataprepper.plugins.processor.utils.LicenseTypeCheck;

import java.net.InetAddress;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeoIPProcessorService {

    private GeoIPProcessorConfig geoIPProcessorConfig;
    private DownloadSource downloadSource;
    private LicenseTypeOptions licenseType;
    private GetGeoData geoData;
    private List<DatabasePathURLConfig> databasePath;
    private String tempPath;

    public GeoIPProcessorService(GeoIPProcessorConfig geoIPProcessorConfig, String tempPath) {
        this.geoIPProcessorConfig = geoIPProcessorConfig;
        this.tempPath = tempPath;
        this.databasePath = geoIPProcessorConfig.getServiceType().getMaxMindService().getDatabasePath();
        downloadThroughURLandS3(DbSourceIdentification.getDatabasePathType(geoIPProcessorConfig.getServiceType().getMaxMindService().getDatabasePath()));
    }

    public void downloadThroughURLandS3(DownloadSourceOptions downloadSourceOptions) {

        try
        {
            switch (downloadSourceOptions) {
                case URL:
                    downloadSource = new DownloadThroughUrl();
                    downloadSource.initiateDownload(databasePath);
                    break;
                case S3:
                    downloadSource = new DownloadThroughS3(geoIPProcessorConfig);
                    downloadSource.initiateDownload(databasePath);
                    break;
                case PATH:
                    downloadSource = new DownloadThroughLocalPath(geoIPProcessorConfig, tempPath);
                    downloadSource.initiateDownload(databasePath);
                    break;
            }
        }catch (Exception ex) {}
    }

    public Map<String, Object> getGeoData(InetAddress inetAddress, List<String> attributes , ZonedDateTime pluginStartDateTime) {

        Map<String, Object> getGeoData = new HashMap<>();
        int cacheSize = geoIPProcessorConfig.getServiceType().getMaxMindService().getCacheSize();
        licenseType = LicenseTypeCheck.isGeoLite2OrEnterpriseLicense(tempPath);
        if (licenseType.equals(LicenseTypeOptions.FREE)) {

            geoData = new GetGeoLite2Data(tempPath, cacheSize, pluginStartDateTime, geoIPProcessorConfig);
            getGeoData = geoData.getGeoData(inetAddress, attributes);
        }
        else if (licenseType.equals(LicenseTypeOptions.ENTERPRISE)) {

            geoData = new GetGeoIP2Data(tempPath, cacheSize, pluginStartDateTime, geoIPProcessorConfig);
            getGeoData = geoData.getGeoData(inetAddress, attributes);
        }
        return getGeoData;
    }
}
