/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.plugins.kafkaconnect.source;

import org.apache.kafka.connect.runtime.WorkerConfig;
import org.opensearch.dataprepper.metrics.PluginMetrics;
import org.opensearch.dataprepper.model.buffer.Buffer;
import org.opensearch.dataprepper.model.configuration.PipelineDescription;
import org.opensearch.dataprepper.model.record.Record;
import org.opensearch.dataprepper.model.source.Source;
import org.opensearch.dataprepper.plugins.kafka.extension.KafkaClusterConfigSupplier;
import org.opensearch.dataprepper.plugins.kafka.util.KafkaSecurityConfigurer;
import org.opensearch.dataprepper.plugins.kafkaconnect.configuration.ConnectorConfig;
import org.opensearch.dataprepper.plugins.kafkaconnect.extension.KafkaConnectConfig;
import org.opensearch.dataprepper.plugins.kafkaconnect.extension.KafkaConnectConfigSupplier;
import org.opensearch.dataprepper.plugins.kafkaconnect.extension.WorkerProperties;
import org.opensearch.dataprepper.plugins.kafkaconnect.util.Connector;
import org.opensearch.dataprepper.plugins.kafkaconnect.util.KafkaConnect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * The abstraction of the kafka connect source.
 * The kafka connect and connectors are configured and runs async here.
 */
@SuppressWarnings("deprecation")
public abstract class KafkaConnectSource implements Source<Record<Object>> {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaConnectSource.class);
    public final ConnectorConfig connectorConfig;
    private final String pipelineName;
    private KafkaConnectConfig kafkaConnectConfig;
    private KafkaConnect kafkaConnect;

    public KafkaConnectSource(final ConnectorConfig connectorConfig,
                              final PluginMetrics pluginMetrics,
                              final PipelineDescription pipelineDescription,
                              final KafkaClusterConfigSupplier kafkaClusterConfigSupplier,
                              final KafkaConnectConfigSupplier kafkaConnectConfigSupplier) {
        this.connectorConfig = connectorConfig;
        this.pipelineName = pipelineDescription.getPipelineName();
        if (shouldStartKafkaConnect()) {
            if (kafkaClusterConfigSupplier == null || kafkaConnectConfigSupplier == null) {
                throw new IllegalArgumentException("Extensions: KafkaClusterConfig and KafkaConnectConfig cannot be null");
            }
            this.kafkaConnectConfig = kafkaConnectConfigSupplier.getConfig();
            this.updateConfig(kafkaClusterConfigSupplier);
            this.kafkaConnect = KafkaConnect.getPipelineInstance(
                    pipelineName,
                    pluginMetrics,
                    kafkaConnectConfig.getConnectStartTimeout(),
                    kafkaConnectConfig.getConnectorStartTimeout());
        }
    }

    @Override
    public void start(Buffer<Record<Object>> buffer) {
        if (shouldStartKafkaConnect()) {
            LOG.info("Starting Kafka Connect Source for pipeline: {}", pipelineName);
            // Please make sure buildWokerProperties is always first to execute.
            final WorkerProperties workerProperties = this.kafkaConnectConfig.getWorkerProperties();
            Map<String, String> workerProps = workerProperties.buildKafkaConnectPropertyMap();
            if (workerProps.get(WorkerConfig.BOOTSTRAP_SERVERS_CONFIG) == null || workerProps.get(WorkerConfig.BOOTSTRAP_SERVERS_CONFIG).isEmpty()) {
                throw new IllegalArgumentException("Bootstrap Servers cannot be null or empty");
            }
            final List<Connector> connectors = this.connectorConfig.buildConnectors();
            kafkaConnect.addConnectors(connectors);
            kafkaConnect.initialize(workerProps);
            kafkaConnect.start();
        }
    }

    @Override
    public void stop() {
        if (shouldStartKafkaConnect()) {
            LOG.info("Stopping Kafka Connect Source for pipeline: {}", pipelineName);
            kafkaConnect.stop();
        }
    }

    public boolean shouldStartKafkaConnect() {
        return true;
    }

    private void updateConfig(final KafkaClusterConfigSupplier kafkaClusterConfigSupplier) {
        if (kafkaConnectConfig.getBootstrapServers() == null) {
            this.kafkaConnectConfig.setBootstrapServers(kafkaClusterConfigSupplier.getBootStrapServers());
        }
        if (kafkaConnectConfig.getAuthConfig() == null) {
            kafkaConnectConfig.setAuthConfig(kafkaClusterConfigSupplier.getAuthConfig());
        }
        if (kafkaConnectConfig.getAwsConfig() == null) {
            kafkaConnectConfig.setAwsConfig(kafkaClusterConfigSupplier.getAwsConfig());
        }
        if (kafkaConnectConfig.getEncryptionConfig() == null) {
            kafkaConnectConfig.setEncryptionConfig(kafkaClusterConfigSupplier.getEncryptionConfig());
        }
        Properties authProperties = new Properties();
        KafkaSecurityConfigurer.setAuthProperties(authProperties, kafkaConnectConfig, LOG);
        this.kafkaConnectConfig.setAuthProperties(authProperties);
        // Update Connector Config
        if (Objects.nonNull(kafkaConnectConfig.getBootstrapServers())) {
            this.connectorConfig.setBootstrapServers(String.join(",", kafkaConnectConfig.getBootstrapServers()));
        }
        this.connectorConfig.setAuthProperties(authProperties);
    }
}
