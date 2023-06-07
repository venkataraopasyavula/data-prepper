# GeoIP Processor

This is the Data Prepper GeoIP processor plugin which can enrich Data Prepper events with location information using a provided IP address.
Additionally, this plugin should be able to use either a MaxMind GeoIP Lite2 database or the GeoIP2 Commercial Licensing database. 
The Data Prepper author must provide information for configuring the commercial license.


## Usages

The GeoIP processor should be configured as part of Data Prepper pipeline yaml file.

## Configuration Options

```
pipeline:
  ...
  processor:
    - geoip:
        aws:
          region: us-east-1
          sts_role_arn: arn:aws:iam::123456789012:role/Data-Prepper                  
        keys:
          - key:
              source: "/peer/ip"
              target: "target1"
          - key:
              source: "/peer/ip2"
              target: "target2"
              attributes: ["city_name","country_name"]
        service_type:
          maxmind:
            database_path:
              - url: 
            load_type: "in_memory"
            cache_size: 4096
            cache_refresh_schedule: P30D
```

## AWS Configuration

- `region` (Optional) : The AWS region to use for credentials. Defaults to [standard SDK behavior to determine the region](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/region-selection.html).

- `sts_role_arn` (Optional) : The AWS STS role to assume for requests to S3. which will use the [standard SDK behavior for credentials](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html). 

## Properties Configuration

- `keys` (Required) : List of properties like source, target and attributes can be specified where the location fields are written

- `source` (Required) : source IP for which enrichment will be done. Public IP can be either IPV4 or IPV6.

- `target` (Optional) : Property used to specify the key for the enriched fields. 

- `attributes` (Optional) : Used to specify the properties which are included in the enrichment of data. By default all attributes are considered.  

## Service type Configuration

- `database_path` (Required) :  Used to provide either S3 path, maxmind URL or local file path where the .mmdb file is available.

- `url` (Required) : Provide URL for all three S3, maxmind URL or local file path. 

- `load_type` (Required) :  Load type used for better performance while enrich the data. There are two type load_type are present i.e "in_memory" or "cache".

- `cache_size` (Optional) : Used to mention the cache size. Default cache size is 2MB. Cache size applicable when load_type is cache. 

- `cache_refresh_schedule` (Required) : Switch the DatabaseReader when ever Refresh schedule threshold is met. 


## Developer Guide

This plugin is compatible with Java 11. See below

- [CONTRIBUTING](https://github.com/opensearch-project/data-prepper/blob/main/CONTRIBUTING.md)
- [monitoring](https://github.com/opensearch-project/data-prepper/blob/main/docs/monitoring.md)

The integration tests for this plugin do not run as part of the Data Prepper build.

The following command runs the integration tests:

```
./gradlew :data-prepper-plugins:geoip-processor:integrationTest -Dtests.geoipprocessor.region=<your-aws-region> -Dtests.geoipprocessor.bucket=<your-bucket>
```
