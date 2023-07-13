/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.plugins.source;

import com.linecorp.armeria.client.retry.Backoff;
import io.micrometer.core.instrument.DistributionSummary;
import org.junit.jupiter.api.Disabled;
import org.opensearch.dataprepper.metrics.PluginMetrics;
import org.opensearch.dataprepper.plugins.source.configuration.NotificationSourceOption;
import org.opensearch.dataprepper.plugins.source.configuration.OnErrorOption;
import org.opensearch.dataprepper.plugins.source.configuration.SqsOptions;
import org.opensearch.dataprepper.model.acknowledgements.AcknowledgementSetManager;
import org.opensearch.dataprepper.acknowledgements.DefaultAcknowledgementSetManager;
import org.opensearch.dataprepper.model.acknowledgements.AcknowledgementSet;
import org.opensearch.dataprepper.model.event.Event;
import org.opensearch.dataprepper.model.event.JacksonEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SqsWorkerIT {
    private SqsClient sqsClient;
    @Mock
    private S3Service s3Service;
    private S3SourceConfig s3SourceConfig;
    private PluginMetrics pluginMetrics;
    private S3ObjectGenerator s3ObjectGenerator;
    private String bucket;
    private Backoff backoff;
    private AcknowledgementSetManager acknowledgementSetManager;
    private Double receivedCount = 0.0;
    private Double deletedCount = 0.0;
    private Double ackCallbackCount = 0.0;
    private Event event;
    private AtomicBoolean ready = new AtomicBoolean(false);

    @BeforeEach
    void setUp() {
        acknowledgementSetManager = mock(AcknowledgementSetManager.class);
        final S3Client s3Client = S3Client.builder()
                .region(Region.of(System.getProperty("tests.s3source.region")))
                .build();
        bucket = System.getProperty("tests.s3source.bucket");
        s3ObjectGenerator = new S3ObjectGenerator(s3Client, bucket);

        sqsClient = SqsClient.builder()
                .region(Region.of(System.getProperty("tests.s3source.region")))
                .build();

        backoff = Backoff.exponential(SqsService.INITIAL_DELAY, SqsService.MAXIMUM_DELAY).withJitter(SqsService.JITTER_RATE)
                .withMaxAttempts(Integer.MAX_VALUE);

        s3SourceConfig = mock(S3SourceConfig.class);
        s3Service = mock(S3Service.class);

        pluginMetrics = mock(PluginMetrics.class);
        final Counter sharedCounter = mock(Counter.class);
        final DistributionSummary distributionSummary = mock(DistributionSummary.class);
        final Timer sqsMessageDelayTimer = mock(Timer.class);

        lenient().when(pluginMetrics.counter(anyString())).thenReturn(sharedCounter);
        lenient().when(pluginMetrics.summary(anyString())).thenReturn(distributionSummary);
        when(pluginMetrics.timer(anyString())).thenReturn(sqsMessageDelayTimer);

        final SqsOptions sqsOptions = mock(SqsOptions.class);
        when(sqsOptions.getSqsUrl()).thenReturn(System.getProperty("tests.s3source.queue.url"));
        when(sqsOptions.getVisibilityTimeout()).thenReturn(Duration.ofSeconds(60));
        when(sqsOptions.getMaximumMessages()).thenReturn(10);
        when(sqsOptions.getWaitTime()).thenReturn(Duration.ofSeconds(10));
        when(s3SourceConfig.getSqsOptions()).thenReturn(sqsOptions);
        lenient().when(s3SourceConfig.getOnErrorOption()).thenReturn(OnErrorOption.DELETE_MESSAGES);
        when(s3SourceConfig.getNotificationSource()).thenReturn(NotificationSourceOption.S3);
    }

    private SqsWorker createObjectUnderTest() {
        return new SqsWorker(acknowledgementSetManager, sqsClient, s3Service, s3SourceConfig, pluginMetrics, backoff);
    }

    @AfterEach
    void processRemainingMessages() {
        final SqsWorker objectUnderTest = createObjectUnderTest();
        int sqsMessagesProcessed;
        do {
            sqsMessagesProcessed = objectUnderTest.processSqsMessages();
        }
        while (sqsMessagesProcessed > 0);
    }

    /**
     * receiveMessage of SQS doesn't return the exact number of objects that are written to S3 even if long polling is enabled with
     * MaxNumberOfMessages greater than the number of objects written.
     * The default behaviour is it returns the message immediately as soon as there's a single message and can return upto MaxNumberOfMessages.
     * So the asserts in this test verify at least one message is returned.
     */
    @ParameterizedTest
    @ValueSource(ints = {1, 5})
    void processSqsMessages_should_return_at_least_one_message(final int numberOfObjectsToWrite) throws IOException {
        writeToS3(numberOfObjectsToWrite);

        final SqsWorker objectUnderTest = createObjectUnderTest();
        final int sqsMessagesProcessed = objectUnderTest.processSqsMessages();

        final ArgumentCaptor<S3ObjectReference> s3ObjectReferenceArgumentCaptor = ArgumentCaptor.forClass(S3ObjectReference.class);
        verify(s3Service, atLeastOnce()).addS3Object(s3ObjectReferenceArgumentCaptor.capture(), eq(null));

        assertThat(s3ObjectReferenceArgumentCaptor.getValue().getBucketName(), equalTo(bucket));
        assertThat(s3ObjectReferenceArgumentCaptor.getValue().getKey(), startsWith("s3 source/sqs/"));
        assertThat(sqsMessagesProcessed, greaterThanOrEqualTo(1));
        assertThat(sqsMessagesProcessed, lessThanOrEqualTo(numberOfObjectsToWrite));
    }

    @ParameterizedTest
    @ValueSource(ints = {1})
    void processSqsMessages_should_return_at_least_one_message_with_acks_with_callback_invoked_after_processS3Object_finishes(final int numberOfObjectsToWrite) throws IOException, InterruptedException {
        writeToS3(numberOfObjectsToWrite);

        when(s3SourceConfig.getAcknowledgements()).thenReturn(true);
        final Counter receivedCounter = mock(Counter.class);
        final Counter deletedCounter = mock(Counter.class);
        final Counter ackCallbackCounter = mock(Counter.class);
        when(pluginMetrics.counter(SqsWorker.SQS_MESSAGES_RECEIVED_METRIC_NAME)).thenReturn(receivedCounter);
        when(pluginMetrics.counter(SqsWorker.SQS_MESSAGES_DELETED_METRIC_NAME)).thenReturn(deletedCounter);
        when(pluginMetrics.counter(SqsWorker.ACKNOWLEDGEMENT_SET_CALLACK_METRIC_NAME)).thenReturn(ackCallbackCounter);
        lenient().doAnswer((val) -> {
            receivedCount += (double)val.getArgument(0);
            return null;
        }).when(receivedCounter).increment(any(Double.class));
        lenient().doAnswer((val) -> {
            if (val.getArgument(0) != null) {
                deletedCount += (double)val.getArgument(0);
            }
            return null;
        }).when(deletedCounter).increment(any(Double.class));
        lenient().doAnswer((val) -> {
            ackCallbackCount += 1;
            return null;
        }).when(ackCallbackCounter).increment();

        doAnswer((val) -> {
            AcknowledgementSet ackSet = val.getArgument(1);
            S3ObjectReference s3ObjectReference = val.getArgument(0);
            assertThat(s3ObjectReference.getBucketName(), equalTo(bucket));
            assertThat(s3ObjectReference.getKey(), startsWith("s3 source/sqs/"));
            event = (Event)JacksonEvent.fromMessage(val.getArgument(0).toString());
            ackSet.add(event);
            return null;
        }).when(s3Service).addS3Object(any(S3ObjectReference.class), any(AcknowledgementSet.class));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        acknowledgementSetManager = new  DefaultAcknowledgementSetManager(executor);
        final SqsWorker objectUnderTest = createObjectUnderTest();
        Thread sinkThread = new Thread(() -> {
            try {
                synchronized(this) {
                    while (!ready.get()) {
                        Thread.sleep(100);
                        this.wait();
                    }
                    if (event.getEventHandle() != null) {
                        event.getEventHandle().release(true);
                    }
                }
            } catch (Exception e){}
        });
        sinkThread.start();
        final int sqsMessagesProcessed = objectUnderTest.processSqsMessages();
        synchronized(this) {
            ready.set(true);
            this.notify();
        }
        Thread.sleep(10000);

        assertThat(deletedCount, equalTo((double)1.0));
        assertThat(ackCallbackCount, equalTo((double)1.0));
    }

    @ParameterizedTest
    @ValueSource(ints = {1})
    void processSqsMessages_should_return_at_least_one_message_with_acks_with_callback_invoked_before_processS3Object_finishes(final int numberOfObjectsToWrite) throws IOException, InterruptedException {
        writeToS3(numberOfObjectsToWrite);

        when(s3SourceConfig.getAcknowledgements()).thenReturn(true);
        final Counter receivedCounter = mock(Counter.class);
        final Counter deletedCounter = mock(Counter.class);
        final Counter ackCallbackCounter = mock(Counter.class);
        when(pluginMetrics.counter(SqsWorker.SQS_MESSAGES_RECEIVED_METRIC_NAME)).thenReturn(receivedCounter);
        when(pluginMetrics.counter(SqsWorker.SQS_MESSAGES_DELETED_METRIC_NAME)).thenReturn(deletedCounter);
        when(pluginMetrics.counter(SqsWorker.ACKNOWLEDGEMENT_SET_CALLACK_METRIC_NAME)).thenReturn(ackCallbackCounter);
        lenient().doAnswer((val) -> {
            receivedCount += (double)val.getArgument(0);
            return null;
        }).when(receivedCounter).increment(any(Double.class));
        lenient().doAnswer((val) -> {
            if (val.getArgument(0) != null) {
                deletedCount += (double)val.getArgument(0);
            }
            return null;
        }).when(deletedCounter).increment(any(Double.class));
        lenient().doAnswer((val) -> {
            ackCallbackCount += 1;
            return null;
        }).when(ackCallbackCounter).increment();

        doAnswer((val) -> {
            AcknowledgementSet ackSet = val.getArgument(1);
            S3ObjectReference s3ObjectReference = val.getArgument(0);
            assertThat(s3ObjectReference.getBucketName(), equalTo(bucket));
            assertThat(s3ObjectReference.getKey(), startsWith("s3 source/sqs/"));
            event = (Event)JacksonEvent.fromMessage(val.getArgument(0).toString());

            ackSet.add(event);
            synchronized(this) {
                ready.set(true);
                this.notify();
            }
            try {
                Thread.sleep(4000);
            } catch (Exception e){}

            return null;
        }).when(s3Service).addS3Object(any(S3ObjectReference.class), any(AcknowledgementSet.class));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        acknowledgementSetManager = new  DefaultAcknowledgementSetManager(executor);
        final SqsWorker objectUnderTest = createObjectUnderTest();
        Thread sinkThread = new Thread(() -> {
            try {
                synchronized(this) {
                    while (!ready.get()) {
                        Thread.sleep(100);
                        this.wait();
                    }
                    if (event.getEventHandle() != null) {
                        event.getEventHandle().release(true);
                    }
                }
            } catch (Exception e){}
        });
        sinkThread.start();
        final int sqsMessagesProcessed = objectUnderTest.processSqsMessages();

        Thread.sleep(10000);

        assertThat(deletedCount, equalTo((double)1.0));
        assertThat(ackCallbackCount, equalTo((double)1.0));
    }

    /** The EventBridge test is disabled by default
     * To run this test run only this one test with S3 bucket configured to use EventBridge to send notifications to SQS
    */
    @ParameterizedTest
    @ValueSource(ints = {1, 5})
    @Disabled
    void processSqsMessages_should_return_at_least_one_message_when_using_eventbridge(final int numberOfObjectsToWrite) throws IOException {
        when(s3SourceConfig.getNotificationSource()).thenReturn(NotificationSourceOption.EVENTBRIDGE);
        writeToS3(numberOfObjectsToWrite);

        final SqsWorker objectUnderTest = createObjectUnderTest();
        final int sqsMessagesProcessed = objectUnderTest.processSqsMessages();

        final ArgumentCaptor<S3ObjectReference> s3ObjectReferenceArgumentCaptor = ArgumentCaptor.forClass(S3ObjectReference.class);
        verify(s3Service, atLeastOnce()).addS3Object(s3ObjectReferenceArgumentCaptor.capture(), eq(null));

        assertThat(s3ObjectReferenceArgumentCaptor.getValue().getBucketName(), equalTo(bucket));
        assertThat(s3ObjectReferenceArgumentCaptor.getValue().getKey(), startsWith("s3 source/sqs/"));
        assertThat(sqsMessagesProcessed, greaterThanOrEqualTo(1));
        assertThat(sqsMessagesProcessed, lessThanOrEqualTo(numberOfObjectsToWrite));
    }

    @Test
    void processSqsMessages_should_return_zero_if_no_objects_are_written() {
        final SqsWorker objectUnderTest = createObjectUnderTest();
        final int sqsMessagesProcessed = objectUnderTest.processSqsMessages();

        assertThat(sqsMessagesProcessed, equalTo(0));
    }

    private void writeToS3(final int numberOfObjectsToWrite) throws IOException {
        final int numberOfRecords = 100;
        final NewlineDelimitedRecordsGenerator newlineDelimitedRecordsGenerator = new NewlineDelimitedRecordsGenerator();
        for (int i = 0; i < numberOfObjectsToWrite; i++) {
            final String key = "s3 source/sqs/" + UUID.randomUUID() + "_" + Instant.now().toString() + newlineDelimitedRecordsGenerator.getFileExtension();
            // isCompressionEnabled is set to false since we test for compression in S3ObjectWorkerIT
            s3ObjectGenerator.write(numberOfRecords, key, newlineDelimitedRecordsGenerator, false);
        }
    }
}
