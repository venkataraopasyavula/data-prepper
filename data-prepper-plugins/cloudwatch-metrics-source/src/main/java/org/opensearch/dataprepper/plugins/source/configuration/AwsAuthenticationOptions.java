/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.plugins.source.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import software.amazon.awssdk.regions.Region;

/**
 * Configuration class for AWS authentication to get Region and ARN
 */
public class AwsAuthenticationOptions {
    @JsonProperty("region")
    @Size(min = 1, message = "Region cannot be empty string")
    private String awsRegion;
    @JsonProperty("sts_role_arn")
    @Size(min = 20, max = 2048, message = "awsStsRoleArn length should be between 1 and 2048 characters")
    private String awsStsRoleArn;

    /**
     * Get the region
     * @return region
     */
    public Region getAwsRegion() {
        return awsRegion != null ? Region.of(awsRegion) : null;
    }

    /**
     * Get the role ARN
     * @return role ARN
     */
    public String getAwsStsRoleArn() {
        return awsStsRoleArn;
    }
}