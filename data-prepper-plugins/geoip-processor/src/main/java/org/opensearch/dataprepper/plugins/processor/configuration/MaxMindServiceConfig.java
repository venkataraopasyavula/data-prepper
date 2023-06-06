/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.plugins.processor.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import org.opensearch.dataprepper.plugins.processor.loadtype.LoadTypeOptions;

import java.time.Duration;
import java.util.List;

public class MaxMindServiceConfig {

    @JsonProperty("database_path")
    @NotNull
    List<DatabasePathURLConfig> databasePath;

    @JsonProperty("load_type")
    @NotNull
    private LoadTypeOptions loadType;

    @JsonProperty("cache_size")
    private Integer cacheSize;

    @JsonProperty("cache_refresh_schedule")
    @NotNull
    private Duration cacheRefreshSchedule;


    /**
     * Get the list of Configured Database path options
     * @return List<DatabasePathURLConfig>
     */
    public List<DatabasePathURLConfig> getDatabasePath() {
        return databasePath;
    }

    /**
     * Get the Configured load type either Cache or inmemory
     * @return String
     */
    public LoadTypeOptions getLoadType() {
        return loadType;
    }

    /**
     * Get the Configured Cache size
     * @return Integer
     */
    public Integer getCacheSize() {
        return cacheSize;
    }

    /**
     * Get the Configured Cache refresh scheduled Duration
     * @return Duration
     */
    public Duration getCacheRefreshSchedule() {
        return cacheRefreshSchedule;
    }

}