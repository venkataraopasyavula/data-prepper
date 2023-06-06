/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.plugins.processor;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.opensearch.dataprepper.plugins.processor.configuration.KeysConfig;
import org.opensearch.dataprepper.plugins.processor.configuration.ServiceTypeOptions;
import org.opensearch.dataprepper.plugins.processor.configuration.AwsAuthenticationOptions;

import java.util.List;

/**
 * An implementation class of GeoIP Processor configuration
 */
public class GeoIPProcessorConfig {

    @JsonProperty("aws")
    @NotNull
    @Valid
    private AwsAuthenticationOptions awsAuthenticationOptions;

    @JsonProperty("keys")
    @NotNull
    private List<KeysConfig> keysConfig;

    @JsonProperty("service_type")
    @NotNull
    private ServiceTypeOptions serviceType;

    /**
     * Aws Authentication configuration Options
     * @return AwsAuthenticationOptions
     */
    public AwsAuthenticationOptions getAwsAuthenticationOptions() {
        return awsAuthenticationOptions;
    }

    /**
     * Lists of Source, target and attributes
     * @return List<KeysConfig>
     */
    public List<KeysConfig> getKeysConfig() {
        return keysConfig;
    }

    /**
     * Service type Options
     * @return ServiceTypeOptions
     */
    public ServiceTypeOptions getServiceType() {
        return serviceType;
    }
}
