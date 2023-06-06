/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.plugins.processor.utils;

import org.opensearch.dataprepper.plugins.processor.configuration.DatabasePathURLConfig;
import org.opensearch.dataprepper.plugins.processor.databasedownload.DownloadSourceOptions;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

public class DbSourceIdentification {

    private DbSourceIdentification() {}
    private static String s3DomainPattern = "[a-zA-Z0-9-]+\\.s3\\.amazonaws\\.com";

    public static boolean isS3Uri(String uriString) {
        try {
            URI uri = new URI(uriString);
            if (uri.getScheme() != null && uri.getScheme().equalsIgnoreCase("s3")) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isS3Url(String urlString) {
        try {
            URL url = new URL(urlString);
            if (Pattern.matches(s3DomainPattern, url.getHost())) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public static boolean isURL(String input) {
        try {
            URI uri = new URI(input);
            URL url = new URL(input);
            return uri.getScheme() != null && !Pattern.matches(s3DomainPattern, url.getHost()) &&(uri.getScheme().equals("http") || uri.getScheme().equals("https"));
        } catch (URISyntaxException | MalformedURLException e) {
            return false;
        }
    }
    public static boolean isFilePath(String input) {
        return input.startsWith("/") || input.startsWith("\\") || (input.length() > 1 && input.charAt(1) == ':');
    }
    public static DownloadSourceOptions getDatabasePathType(List<DatabasePathURLConfig> dbPath) {

        DownloadSourceOptions downloadSourceOptions = null;
        for( DatabasePathURLConfig path : dbPath) {

            if(DbSourceIdentification.isFilePath(path.getUrl())) {
                return DownloadSourceOptions.PATH;
            }
            else if(DbSourceIdentification.isURL(path.getUrl()))
            {
                downloadSourceOptions = DownloadSourceOptions.URL;
            }
            else if(DbSourceIdentification.isS3Uri(path.getUrl()) || (DbSourceIdentification.isS3Url(path.getUrl())))
            {
                downloadSourceOptions = DownloadSourceOptions.S3;
            }
        }
        return downloadSourceOptions;
    }
}
