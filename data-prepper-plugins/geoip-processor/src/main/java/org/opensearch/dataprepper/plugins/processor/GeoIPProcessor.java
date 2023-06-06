/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.plugins.processor;

import java.time.*;
import java.util.concurrent.locks.Lock;

import org.opensearch.dataprepper.model.annotations.DataPrepperPlugin;
import org.opensearch.dataprepper.model.annotations.DataPrepperPluginConstructor;
import org.opensearch.dataprepper.model.configuration.PluginSetting;
import org.opensearch.dataprepper.model.event.Event;
import io.micrometer.core.instrument.Counter;
import org.opensearch.dataprepper.model.plugin.PluginFactory;
import org.opensearch.dataprepper.model.processor.AbstractProcessor;
import org.opensearch.dataprepper.model.processor.Processor;
import org.opensearch.dataprepper.model.record.Record;
import org.opensearch.dataprepper.plugins.processor.configuration.KeysConfig;
import org.opensearch.dataprepper.plugins.processor.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.Timer;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@DataPrepperPlugin(name = "geoip", pluginType = Processor.class, pluginConfigurationType = GeoIPProcessorConfig.class)
public class GeoIPProcessor extends AbstractProcessor<Record<Event>, Record<Event>> {
  private static final Logger LOG = LoggerFactory.getLogger(GeoIPProcessor.class);
  private static final String GEO_IP_PROCESSING_MATCH = "geoIpProcessingMatch";
  private static final String GEO_IP_PROCESSING_MISMATCH = "geoIpProcessingMismatch";
  private static final String GEO_IP_PROCESSING_ERRORS = "geoIpProcessingErrors";
  private static final String GEO_IP_PROCESSING_TIMEOUTS = "geoIpProcessingTimeouts";
  private static final String GEO_IP_PROCESSING_TIME = "geoIpProcessingTime";

  private final Counter geoIpProcessingMatchCounter;
  private final Counter geoIpProcessingMismatchCounter;
  private final Counter geoIpProcessingErrorsCounter;
  private final Counter geoIpProcessingTimeoutsCounter;
  private final Timer geoIpProcessingTime;
  private final GeoIPProcessorConfig geoIPProcessorConfig;
  private final Lock reentrantLock;
  private String tempPath;
  private static final String TAGS_ON_MATCH_FAILURE = "tags_on_match_failure";
  private final Duration scheduledInterval;
  private final List<String> tagsOnMatchFailure;
  private GeoIPProcessorService geoIPProcessorService;

  private ZonedDateTime pluginStartDateTime;

  @DataPrepperPluginConstructor
  public GeoIPProcessor(PluginSetting pluginSetting,
                        final GeoIPProcessorConfig geoCodingProcessorConfig,
                        final PluginFactory pluginFactory) throws MalformedURLException {
    super(pluginSetting);
    this.geoIPProcessorConfig = geoCodingProcessorConfig;
    scheduledInterval = geoCodingProcessorConfig.getServiceType().getMaxMindService().getCacheRefreshSchedule();

    this.tempPath = System.getProperty("java.io.tmpdir")+ File.separator +"GeoIP";
    reentrantLock = new ReentrantLock();
    geoIPProcessorService = new GeoIPProcessorService(geoCodingProcessorConfig,tempPath);
    this.pluginStartDateTime = LocalDateTime.now().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of(TimeZone.getTimeZone("UTC").getID()));

    tagsOnMatchFailure = pluginSetting.getTypedList(TAGS_ON_MATCH_FAILURE, String.class);
    geoIpProcessingMatchCounter = pluginMetrics.counter(GEO_IP_PROCESSING_MATCH);
    geoIpProcessingMismatchCounter = pluginMetrics.counter(GEO_IP_PROCESSING_MISMATCH);
    geoIpProcessingErrorsCounter = pluginMetrics.counter(GEO_IP_PROCESSING_ERRORS);
    geoIpProcessingTimeoutsCounter = pluginMetrics.counter(GEO_IP_PROCESSING_TIMEOUTS);
    geoIpProcessingTime = pluginMetrics.timer(GEO_IP_PROCESSING_TIME);

  }

  @Override
  public Collection<Record<Event>> doExecute(Collection<Record<Event>> records) {

    reentrantLock.lock();
    Map<String, Object> geoData = new HashMap<>();

    for (final Record<Event> record : records) {
      LOG.info("Venkat Before Enrich---", record.getData());
      Event event = record.getData();
        for (KeysConfig key : geoIPProcessorConfig.getKeysConfig()) {
          String source = key.getKeyConfig().getSource();
          List<String> attributes = key.getKeyConfig().getAttributes();
          String ipAddress = event.get(source, String.class);

            //Lookup from DB
            if (ipAddress != null && (!(ipAddress.isEmpty()))) {
            try {
                   if(!(IPValidationcheck.ipValidationcheck(ipAddress))) {
                     InetAddress inetAddress = InetAddress.getByName(ipAddress);

                     geoData = geoIPProcessorService.getGeoData(inetAddress, attributes, pluginStartDateTime);
                     record.getData().put(key.getKeyConfig().getTarget(), geoData);
                     geoIpProcessingMatchCounter.increment();
                   }
            } catch (IOException e) {
              geoIpProcessingMismatchCounter.increment();
             // event.getMetadata().addTags(tagsOnMatchFailure);
              throw new RuntimeException(e);
            }
          }
          else {
          //  event.getMetadata().addTags(tagsOnMatchFailure);
            //No Enrichment. continue for next IP
            continue;
          }
        }
        LOG.info("Venkat After Enrich---", record.getData());
    }
    reentrantLock.unlock();

    return records;
  }

  @Override
  public void prepareForShutdown() {
    LOG.info("prepareForShutdown");
  }

  @Override
  public boolean isReadyForShutdown() {
    LOG.info("isReadyForShutdown");
    return false;
  }

  @Override
  public void shutdown() {
    LOG.info("shutdown");
  }
}