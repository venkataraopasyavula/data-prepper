package org.opensearch.dataprepper.plugins.processor.databasedownload;

import org.opensearch.dataprepper.plugins.processor.GeoIPProcessorConfig;
import org.opensearch.dataprepper.plugins.processor.configuration.DatabasePathURLConfig;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;


public class DownloadThroughLocalPath implements DownloadSource {

    private GeoIPProcessorConfig geoIPProcessorConfig;
    private String tempPath;
    public DownloadThroughLocalPath(GeoIPProcessorConfig geoIPProcessorConfig, String tempPath) {
        this.geoIPProcessorConfig = geoIPProcessorConfig;
        this.tempPath = tempPath;
    }
    @Override
    public void initiateDownload(List<DatabasePathURLConfig> config) throws Exception {
        String sourcePath = geoIPProcessorConfig.getServiceType().getMaxMindService().getDatabasePath().get(0).getUrl();
        for(DatabasePathURLConfig path : config) {
            DownloadSource.createFolderIfNotExist(tempPath);
            File srcDatabaseConfigPath = new File(sourcePath);
            File destDatabaseConfigPath = new File(tempPath);
            FileUtils.copyDirectory(srcDatabaseConfigPath, destDatabaseConfigPath);
        }
    }

    @Override
    public void buildRequestAndDownloadFile(String key) throws Exception {

    }

}
