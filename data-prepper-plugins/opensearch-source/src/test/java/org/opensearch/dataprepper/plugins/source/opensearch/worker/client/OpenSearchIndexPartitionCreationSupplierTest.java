/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.plugins.source.opensearch.worker.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.cat.IndicesResponse;
import org.opensearch.client.opensearch.cat.OpenSearchCatClient;
import org.opensearch.client.opensearch.cat.indices.IndicesRecord;
import org.opensearch.dataprepper.model.source.coordinator.PartitionIdentifier;
import org.opensearch.dataprepper.plugins.source.opensearch.OpenSearchSourceConfiguration;
import org.opensearch.dataprepper.plugins.source.opensearch.configuration.IndexParametersConfiguration;
import org.opensearch.dataprepper.plugins.source.opensearch.configuration.OpenSearchIndex;
import org.opensearch.dataprepper.plugins.source.opensearch.worker.OpenSearchIndexPartitionCreationSupplier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OpenSearchIndexPartitionCreationSupplierTest {

    @Mock
    private OpenSearchSourceConfiguration openSearchSourceConfiguration;

    @Mock
    private ClusterClientFactory clusterClientFactory;

    @Mock
    private OpenSearchClient openSearchClient;

    private OpenSearchIndexPartitionCreationSupplier createObjectUnderTest() {
        return new OpenSearchIndexPartitionCreationSupplier(openSearchSourceConfiguration, clusterClientFactory);
    }

    @Test
    void clusterClientFactory_that_is_not_OpenSearchClient_throws_IllegalArgumentException() {
        when(clusterClientFactory.getClient()).thenReturn(mock(Object.class));

        assertThrows(IllegalArgumentException.class, this::createObjectUnderTest);
    }

    @ParameterizedTest
    @MethodSource("opensearchCatIndicesExceptions")
    void apply_with_opensearch_client_cat_indices_throws_exception_returns_empty_list(final Class exception) throws IOException {
        when(clusterClientFactory.getClient()).thenReturn(openSearchClient);

        final OpenSearchCatClient openSearchCatClient = mock(OpenSearchCatClient.class);
        when(openSearchCatClient.indices()).thenThrow(exception);
        when(openSearchClient.cat()).thenReturn(openSearchCatClient);

        final List<PartitionIdentifier> partitionIdentifierList = createObjectUnderTest().apply(Collections.emptyMap());

        assertThat(partitionIdentifierList, notNullValue());
        assertThat(partitionIdentifierList.isEmpty(), equalTo(true));
    }

    @Test
    void apply_with_opensearch_client_with_no_indices_return_empty_list() throws IOException {
        when(clusterClientFactory.getClient()).thenReturn(openSearchClient);

        final OpenSearchCatClient openSearchCatClient = mock(OpenSearchCatClient.class);
        final IndicesResponse indicesResponse = mock(IndicesResponse.class);
        when(indicesResponse.valueBody()).thenReturn(Collections.emptyList());
        when(openSearchCatClient.indices()).thenReturn(indicesResponse);
        when(openSearchClient.cat()).thenReturn(openSearchCatClient);

        final List<PartitionIdentifier> partitionIdentifierList = createObjectUnderTest().apply(Collections.emptyMap());

        assertThat(partitionIdentifierList, notNullValue());
        assertThat(partitionIdentifierList.isEmpty(), equalTo(true));
    }

    @Test
    void apply_with_opensearch_client_with_indices_filters_them_correctly() throws IOException {
        when(clusterClientFactory.getClient()).thenReturn(openSearchClient);

        final OpenSearchCatClient openSearchCatClient = mock(OpenSearchCatClient.class);
        final IndicesResponse indicesResponse = mock(IndicesResponse.class);

        final IndexParametersConfiguration indexParametersConfiguration = mock(IndexParametersConfiguration.class);

        final List<OpenSearchIndex> includedIndices = new ArrayList<>();
        final OpenSearchIndex includeIndex = mock(OpenSearchIndex.class);
        final String includePattern = "my-pattern-[a-c].*";
        when(includeIndex.getIndexNamePattern()).thenReturn(Pattern.compile(includePattern));
        includedIndices.add(includeIndex);

        final List<OpenSearchIndex> excludedIndices = new ArrayList<>();
        final OpenSearchIndex excludeIndex = mock(OpenSearchIndex.class);
        final String excludePattern = "my-pattern-[a-c]-exclude";
        when(excludeIndex.getIndexNamePattern()).thenReturn(Pattern.compile(excludePattern));
        excludedIndices.add(excludeIndex);

        final OpenSearchIndex secondExcludeIndex = mock(OpenSearchIndex.class);
        final String secondExcludePattern = "second-exclude-.*";
        when(secondExcludeIndex.getIndexNamePattern()).thenReturn(Pattern.compile(secondExcludePattern));
        excludedIndices.add(secondExcludeIndex);

        when(indexParametersConfiguration.getIncludedIndices()).thenReturn(includedIndices);
        when(indexParametersConfiguration.getExcludedIndices()).thenReturn(excludedIndices);
        when(openSearchSourceConfiguration.getIndexParametersConfiguration()).thenReturn(indexParametersConfiguration);

        final List<IndicesRecord> indicesRecords = new ArrayList<>();
        final IndicesRecord includedIndex = mock(IndicesRecord.class);
        when(includedIndex.index()).thenReturn("my-pattern-a-include");
        final IndicesRecord excludedIndex = mock(IndicesRecord.class);
        when(excludedIndex.index()).thenReturn("second-exclude-test");
        final IndicesRecord includedAndThenExcluded = mock(IndicesRecord.class);
        when(includedAndThenExcluded.index()).thenReturn("my-pattern-a-exclude");
        final IndicesRecord neitherIncludedOrExcluded = mock(IndicesRecord.class);
        when(neitherIncludedOrExcluded.index()).thenReturn("random-index");

        indicesRecords.add(includedIndex);
        indicesRecords.add(excludedIndex);
        indicesRecords.add(includedAndThenExcluded);
        indicesRecords.add(neitherIncludedOrExcluded);

        when(indicesResponse.valueBody()).thenReturn(indicesRecords);

        when(openSearchCatClient.indices()).thenReturn(indicesResponse);
        when(openSearchClient.cat()).thenReturn(openSearchCatClient);

        final List<PartitionIdentifier> partitionIdentifierList = createObjectUnderTest().apply(Collections.emptyMap());

        assertThat(partitionIdentifierList, notNullValue());
    }

    private static Stream<Arguments> opensearchCatIndicesExceptions() {
        return Stream.of(Arguments.of(IOException.class),
                Arguments.of(OpenSearchException.class));
    }
}
