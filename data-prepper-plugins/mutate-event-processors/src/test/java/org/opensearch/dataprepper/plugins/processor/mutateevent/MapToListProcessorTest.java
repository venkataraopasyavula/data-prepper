/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.plugins.processor.mutateevent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.dataprepper.expression.ExpressionEvaluator;
import org.opensearch.dataprepper.metrics.PluginMetrics;
import org.opensearch.dataprepper.model.event.Event;
import org.opensearch.dataprepper.model.event.JacksonEvent;
import org.opensearch.dataprepper.model.record.Record;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MapToListProcessorTest {
    @Mock
    private PluginMetrics pluginMetrics;

    @Mock
    private MapToListProcessorConfig mockConfig;

    @Mock
    private ExpressionEvaluator expressionEvaluator;

    @BeforeEach
    void setUp() {
        lenient().when(mockConfig.getSource()).thenReturn("my-map");
        lenient().when(mockConfig.getTarget()).thenReturn("my-list");
        lenient().when(mockConfig.getKeyName()).thenReturn("key");
        lenient().when(mockConfig.getValueName()).thenReturn("value");
        lenient().when(mockConfig.getMapToListWhen()).thenReturn(null);
        lenient().when(mockConfig.getExcludeKeys()).thenReturn(new ArrayList<>());
        lenient().when(mockConfig.getRemoveProcessedFields()).thenReturn(false);
    }

    @Test
    void testMapToListSuccessWithDefaultOptions() {

        final MapToListProcessor processor = createObjectUnderTest();
        final Record<Event> testRecord = createTestRecord();
        final List<Record<Event>> resultRecord = (List<Record<Event>>) processor.doExecute(Collections.singletonList(testRecord));

        assertThat(resultRecord.size(), is(1));

        final Event resultEvent = resultRecord.get(0).getData();
        List<Map<String, Object>> resultList = resultEvent.get("my-list", List.class);

        assertThat(resultList.size(), is(3));
        assertThat(resultList, containsInAnyOrder(
                Map.of("key", "key1", "value", "value1"),
                Map.of("key", "key2", "value", "value2"),
                Map.of("key", "key3", "value", "value3")
        ));
        assertThat(resultEvent.containsKey("my-map"), is(true));
        assertSourceMapUnchanged(resultEvent);
    }

    @Test
    void testMapToListSuccessWithCustomKeyNameValueName() {
        final String keyName = "custom-key-name";
        final String valueName = "custom-value-name";
        when(mockConfig.getKeyName()).thenReturn(keyName);
        when(mockConfig.getValueName()).thenReturn(valueName);

        final MapToListProcessor processor = createObjectUnderTest();
        final Record<Event> testRecord = createTestRecord();
        final List<Record<Event>> resultRecord = (List<Record<Event>>) processor.doExecute(Collections.singletonList(testRecord));

        assertThat(resultRecord.size(), is(1));

        final Event resultEvent = resultRecord.get(0).getData();
        List<Map<String, Object>> resultList = resultEvent.get("my-list", List.class);

        assertThat(resultList.size(), is(3));
        assertThat(resultList, containsInAnyOrder(
                Map.of(keyName, "key1", valueName, "value1"),
                Map.of(keyName, "key2", valueName, "value2"),
                Map.of(keyName, "key3", valueName, "value3")
        ));
        assertSourceMapUnchanged(resultEvent);
    }

    @Test
    void testEventNotProcessedWhenSourceNotExistInEvent() {
        when(mockConfig.getSource()).thenReturn("my-other-map");

        final MapToListProcessor processor = createObjectUnderTest();
        final Record<Event> testRecord = createTestRecord();
        final List<Record<Event>> resultRecord = (List<Record<Event>>) processor.doExecute(Collections.singletonList(testRecord));

        assertThat(resultRecord.size(), is(1));

        final Event resultEvent = resultRecord.get(0).getData();
        assertThat(resultEvent.containsKey("my-list"), is(false));
        assertSourceMapUnchanged(resultEvent);
    }

    @Test
    void testExcludedKeysAreNotProcessed() {
        when(mockConfig.getExcludeKeys()).thenReturn(List.of("key1", "key3", "key5"));

        final MapToListProcessor processor = createObjectUnderTest();
        final Record<Event> testRecord = createTestRecord();
        final List<Record<Event>> resultRecord = (List<Record<Event>>) processor.doExecute(Collections.singletonList(testRecord));

        assertThat(resultRecord.size(), is(1));

        final Event resultEvent = resultRecord.get(0).getData();
        List<Map<String, Object>> resultList = resultEvent.get("my-list", List.class);

        assertThat(resultList.size(), is(1));
        assertThat(resultList.get(0), is(Map.of("key", "key2", "value", "value2")));
        assertSourceMapUnchanged(resultEvent);
    }

    @Test
    void testRemoveProcessedFields() {
        when(mockConfig.getExcludeKeys()).thenReturn(List.of("key1", "key3", "key5"));
        when(mockConfig.getRemoveProcessedFields()).thenReturn(true);

        final MapToListProcessor processor = createObjectUnderTest();
        final Record<Event> testRecord = createTestRecord();
        final List<Record<Event>> resultRecord = (List<Record<Event>>) processor.doExecute(Collections.singletonList(testRecord));

        assertThat(resultRecord.size(), is(1));

        final Event resultEvent = resultRecord.get(0).getData();
        List<Map<String, Object>> resultList = resultEvent.get("my-list", List.class);

        assertThat(resultList.size(), is(1));
        assertThat(resultList.get(0), is(Map.of("key", "key2", "value", "value2")));

        assertThat(resultEvent.containsKey("my-map"), is(true));
        assertThat(resultEvent.containsKey("my-map/key1"), is(true));
        assertThat(resultEvent.get("my-map/key1", String.class), is("value1"));
        assertThat(resultEvent.containsKey("my-map/key2"), is(false));
        assertThat(resultEvent.containsKey("my-map/key3"), is(true));
        assertThat(resultEvent.get("my-map/key3", String.class), is("value3"));
    }

    @Test
    public void testEventNotProcessedWhenTheWhenConditionIsFalse() {
        final String whenCondition = UUID.randomUUID().toString();
        when(mockConfig.getMapToListWhen()).thenReturn(whenCondition);

        final MapToListProcessor processor = createObjectUnderTest();
        final Record<Event> testRecord = createTestRecord();
        when(expressionEvaluator.evaluateConditional(whenCondition, testRecord.getData())).thenReturn(false);
        final List<Record<Event>> resultRecord = (List<Record<Event>>) processor.doExecute(Collections.singletonList(testRecord));

        assertThat(resultRecord.size(), is(1));

        final Event resultEvent = resultRecord.get(0).getData();
        assertThat(resultEvent.containsKey("my-list"), is(false));
        assertSourceMapUnchanged(resultEvent);
    }

    private MapToListProcessor createObjectUnderTest() {
        return new MapToListProcessor(pluginMetrics, mockConfig, expressionEvaluator);
    }

    private Record<Event> createTestRecord() {
        final Map<String, Map<String, Object>> data = Map.of("my-map", Map.of(
                "key1", "value1",
                "key2", "value2",
                "key3", "value3"));
        final Event event = JacksonEvent.builder()
                .withData(data)
                .withEventType("event")
                .build();
        return new Record<>(event);
    }

    private void assertSourceMapUnchanged(final Event resultEvent) {
        assertThat(resultEvent.containsKey("my-map"), is(true));
        assertThat(resultEvent.get("my-map/key1", String.class), is("value1"));
        assertThat(resultEvent.get("my-map/key2", String.class), is("value2"));
        assertThat(resultEvent.get("my-map/key3", String.class), is("value3"));
    }
}
