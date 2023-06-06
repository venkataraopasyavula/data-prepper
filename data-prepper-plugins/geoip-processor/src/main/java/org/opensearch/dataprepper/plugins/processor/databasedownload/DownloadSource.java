package org.opensearch.dataprepper.plugins.processor.databasedownload;

import org.opensearch.dataprepper.plugins.processor.configuration.DatabasePathURLConfig;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

public interface DownloadSource {

    String tempFolderPath = System.getProperty("java.io.tmpdir")+ File.separator +"GeoIP";
    String tarFolderPath = tempFolderPath + "/tar";
    String downloadTarFilepath = tarFolderPath + "/out.tar.gz";

    void initiateDownload(List<DatabasePathURLConfig> config) throws Exception;

    void buildRequestAndDownloadFile(String key) throws Exception;

    /**
     * createFolderIfNotExist
     * @param outputFilePath Output File Path
     * @return File
     */
    static File createFolderIfNotExist(String outputFilePath) {
        final File destFile = new File(outputFilePath);
        if (!destFile.exists()) {
            destFile.mkdir();
        }
        return destFile;
    }

    /**
     * deleteDirectory
     * @param file file
     */
    static void deleteDirectory(File file) {
        for (final File subFile : file.listFiles()) {
            if (subFile.isDirectory()) {
                deleteDirectory(subFile);
            }
            subFile.delete();
        }
    }

    /**
     * initiateSSL
     * @throws NoSuchAlgorithmException NoSuchAlgorithmException
     * @throws KeyManagementException KeyManagementException
     */
    default void initiateSSL() throws NoSuchAlgorithmException, KeyManagementException {
        final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                        return;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                        return;
                    }
                }
        };

        final SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        final HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            public boolean verify(String urlHostName, SSLSession session) {
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
    }
}
