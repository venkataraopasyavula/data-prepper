/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.plugins.processor.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DbSourceIdentificationTest {

    public static final String S3_URI = "s3://dataprepper/logdata/22833bd46b8e0.mmdb";
    public static final String S3_URL = "https://dataprepper.s3.amazonaws.com/logdata/22833bd46b8e0.json";
    public static final String URL = "https://www.dataprepper.com";
    public static final String path = "c://example//file.txt";

    @Test
    void test_positive_case(){
        assertTrue(DbSourceIdentification.isS3Uri(S3_URI));
        assertTrue(DbSourceIdentification.isS3Url(S3_URL));
        assertTrue(DbSourceIdentification.isURL(URL));
        assertTrue(DbSourceIdentification.isFilePath(path));
    }

    @Test
    void test_negative_case(){
        assertFalse(DbSourceIdentification.isS3Uri(S3_URL));
        assertFalse(DbSourceIdentification.isS3Uri(URL));
        assertFalse(DbSourceIdentification.isS3Uri(path));

        assertFalse(DbSourceIdentification.isS3Url(S3_URI));
        assertFalse(DbSourceIdentification.isS3Url(URL));
        assertFalse(DbSourceIdentification.isS3Url(path));

        //assertFalse(DbSourceIdentification.isURL(S3_URI));
        assertFalse(DbSourceIdentification.isURL(S3_URL));
        //assertFalse(DbSourceIdentification.isURL(path));

        assertFalse(DbSourceIdentification.isFilePath(S3_URI));
        assertFalse(DbSourceIdentification.isFilePath(S3_URL));
        assertFalse(DbSourceIdentification.isFilePath(URL));
    }
}
