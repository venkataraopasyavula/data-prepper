package org.opensearch.dataprepper.plugins.processor.databasedownload;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum LicenseTypeOptions {
    FREE("free"),
    ENTERPRISE("enterprise");

    private final String option;

    private static final Map<String, LicenseTypeOptions> OPTIONS_MAP = Arrays.stream(LicenseTypeOptions.values())
            .collect(Collectors.toMap(
                    value -> value.option,
                    value -> value
            ));

    LicenseTypeOptions(final String option) {
        this.option = option;
    }
    @JsonCreator
    static LicenseTypeOptions fromOptionValue(final String option) {
        return OPTIONS_MAP.get(option);
    }
}
