package org.opensearch.dataprepper.plugins.processor.databasedownload;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum DownloadSourceOptions {
    PATH("path"),
    URL("url"),
    S3("s3");

    private final String option;

    private static final Map<String, DownloadSourceOptions> OPTIONS_MAP = Arrays.stream(DownloadSourceOptions.values())
            .collect(Collectors.toMap(
                    value -> value.option,
                    value -> value
            ));

    DownloadSourceOptions(final String option) {
        this.option = option;
    }

    @JsonCreator
    static DownloadSourceOptions fromOptionValue(final String option) {
        return OPTIONS_MAP.get(option);
    }
}
