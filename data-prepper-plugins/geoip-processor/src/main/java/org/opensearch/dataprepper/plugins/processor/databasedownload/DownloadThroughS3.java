package org.opensearch.dataprepper.plugins.processor.databasedownload;


import org.opensearch.dataprepper.plugins.processor.GeoIPProcessorConfig;
import org.opensearch.dataprepper.plugins.processor.configuration.DatabasePathURLConfig;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.DirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.DownloadDirectoryRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class DownloadThroughS3 implements DownloadSource {

    private GeoIPProcessorConfig geoIPProcessorConfig;
    private S3Client client;
    public DownloadThroughS3(GeoIPProcessorConfig geoIPProcessorConfig) {
       this.geoIPProcessorConfig = geoIPProcessorConfig;
    }

    public void initiateDownload(List<DatabasePathURLConfig> s3URLs)  {
        for(DatabasePathURLConfig s3Url : s3URLs) {
            try {
                URI uri = new URI(s3Url.getUrl());
                DownloadSource.createFolderIfNotExist(tempFolderPath);
                buildRequestAndDownloadFile(uri.getHost());
            }
            catch (URISyntaxException ex) {}
        }
    }

    @Override
    public void initiateSSL() throws NoSuchAlgorithmException, KeyManagementException {
        DownloadSource.super.initiateSSL();
    }

    @Override
    public void buildRequestAndDownloadFile(String bucketName)  {

        S3TransferManager transferManager = createCustomTransferManager();
        DirectoryDownload directoryDownload =
                transferManager.downloadDirectory(
                        DownloadDirectoryRequest.builder()
                                .destination(Paths.get(tempFolderPath))
                                .bucket(bucketName)
                                // only download objects with prefix
                                .listObjectsV2RequestTransformer(l -> l.prefix(""))
                                .build());
        // Wait for the transfer to complete
        CompletedDirectoryDownload completedDirectoryDownload = directoryDownload.completionFuture().join();
    }

    public S3TransferManager createCustomTransferManager(){
        S3AsyncClient s3AsyncClient =
                S3AsyncClient.crtBuilder()
                        .region(geoIPProcessorConfig.getAwsAuthenticationOptions().getAwsRegion())
                        .credentialsProvider(geoIPProcessorConfig.getAwsAuthenticationOptions().authenticateAwsConfiguration())
                        .build();

        return S3TransferManager.builder()
                .s3Client(s3AsyncClient)
                .build();
    }
}
