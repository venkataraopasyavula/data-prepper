package org.opensearch.dataprepper.plugins.processor.loadtype;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum LoadTypeOptions {

    INMEMORY("in_memory"),
    CACHE("cache");

    private final String option;
    private static final Map<String, LoadTypeOptions> OPTIONS_MAP = Arrays.stream(LoadTypeOptions.values())
            .collect(Collectors.toMap(
                    value -> value.option,
                    value -> value
            ));

    LoadTypeOptions(final String option) {
        this.option = option;
    }

    @JsonCreator
    static LoadTypeOptions fromOptionValue(final String option) {
        return OPTIONS_MAP.get(option);
    }
}