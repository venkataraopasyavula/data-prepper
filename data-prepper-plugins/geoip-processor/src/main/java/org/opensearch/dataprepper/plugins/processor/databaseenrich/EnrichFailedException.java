package org.opensearch.dataprepper.plugins.processor.databaseenrich;

public class EnrichFailedException extends RuntimeException {

    public EnrichFailedException(String exceptionMsg) {
        super(exceptionMsg);
    }
}
