/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.auth.config;

import com.epam.reportportal.auth.commons.Thumbnailator;
import com.epam.reportportal.auth.commons.ThumbnailatorImpl;
import com.epam.reportportal.auth.filesystem.DataStore;
import com.epam.reportportal.auth.filesystem.LocalDataStore;
import com.epam.reportportal.auth.filesystem.distributed.s3.S3DataStore;
import com.epam.reportportal.auth.util.FeatureFlagHandler;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.jclouds.ContextBuilder;
import org.jclouds.aws.s3.config.AWSS3HttpApiModule;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.ContainerNotFoundException;
import org.jclouds.filesystem.reference.FilesystemConstants;
import org.jclouds.http.HttpRequest;
import org.jclouds.location.reference.LocationConstants;
import org.jclouds.rest.ConfiguresHttpApi;
import org.jclouds.s3.S3Client;
import org.jclouds.s3.config.S3HttpApiModule;
import org.jclouds.s3.filters.Aws4SignerForAuthorizationHeader;
import org.jclouds.s3.filters.Aws4SignerForChunkedUpload;
import org.jclouds.s3.filters.Aws4SignerForQueryString;
import org.jclouds.s3.filters.RequestAuthorizeSignature;
import org.jclouds.s3.filters.RequestAuthorizeSignatureV4;
import org.jclouds.s3.reference.S3Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Dzianis_Shybeka
 */
@Configuration
public class DataStoreConfiguration {

  /**
   * Amazon has a general work flow they publish that allows clients to always find the correct URL
   * endpoint for a given bucket: 1) ask s3.amazonaws.com for the bucket location 2) use the url
   * returned to make the container specific request (get/put, etc) Jclouds cache the results from
   * the first getBucketLocation call and use that region-specific URL, as needed. In this custom
   * implementation of {@link AWSS3HttpApiModule} we are providing location from environment
   * variable, so that we don't need to make getBucketLocation call
   */
  @ConfiguresHttpApi
  private static class CustomBucketToRegionModule extends AWSS3HttpApiModule {

    private final String region;

    public CustomBucketToRegionModule(String region) {
      this.region = region;
    }

    @Override
    @SuppressWarnings("Guava")
    protected CacheLoader<String, Optional<String>> bucketToRegion(
        Supplier<Set<String>> regionSupplier, S3Client client) {
      Set<String> regions = regionSupplier.get();
      if (regions.isEmpty()) {
        return new CacheLoader<>() {

          @Override
          @SuppressWarnings({"Guava", "NullableProblems"})
          public Optional<String> load(String bucket) {
            if (CustomBucketToRegionModule.this.region != null) {
              return Optional.of(CustomBucketToRegionModule.this.region);
            }
            return Optional.absent();
          }

          @Override
          public String toString() {
            return "noRegions()";
          }
        };
      } else if (regions.size() == 1) {
        final String onlyRegion = Iterables.getOnlyElement(regions);
        return new CacheLoader<>() {
          @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
          final Optional<String> onlyRegionOption = Optional.of(onlyRegion);

          @Override
          @SuppressWarnings("NullableProblems")
          public Optional<String> load(String bucket) {
            if (CustomBucketToRegionModule.this.region != null) {
              return Optional.of(CustomBucketToRegionModule.this.region);
            }
            return onlyRegionOption;
          }

          @Override
          public String toString() {
            return "onlyRegion(" + onlyRegion + ")";
          }
        };
      } else {
        return new CacheLoader<>() {
          @Override
          @SuppressWarnings("NullableProblems")
          public Optional<String> load(String bucket) {
            if (CustomBucketToRegionModule.this.region != null) {
              return Optional.of(CustomBucketToRegionModule.this.region);
            }
            try {
              return Optional.fromNullable(client.getBucketLocation(bucket));
            } catch (ContainerNotFoundException e) {
              return Optional.absent();
            }
          }

          @Override
          public String toString() {
            return "bucketToRegion()";
          }
        };
      }
    }
  }


  @Bean
  @ConditionalOnProperty(name = "datastore.type", havingValue = "filesystem")
  public BlobStore filesystemBlobStore(
      @Value("${datastore.path:/data/store}") String baseDirectory) {

    Properties properties = new Properties();
    properties.setProperty(FilesystemConstants.PROPERTY_BASEDIR, baseDirectory);

    BlobStoreContext blobStoreContext =
        ContextBuilder.newBuilder("filesystem").overrides(properties)
            .buildView(BlobStoreContext.class);

    return blobStoreContext.getBlobStore();
  }

  @Bean
  @ConditionalOnProperty(name = "datastore.type", havingValue = "filesystem")
  public DataStore localDataStore(@Autowired BlobStore blobStore,
      FeatureFlagHandler featureFlagHandler,
      @Value("${datastore.bucketPrefix}") String bucketPrefix,
      @Value("${datastore.bucketPostfix}") String bucketPostfix,
      @Value("${datastore.defaultBucketName}") String defaultBucketName) {
    return new LocalDataStore(
        blobStore, featureFlagHandler, bucketPrefix, bucketPostfix, defaultBucketName);
  }

  /**
   * Creates BlobStore bean, that works with MinIO.
   *
   * @param accessKey accessKey to use
   * @param secretKey secretKey to use
   * @param endpoint  MinIO endpoint
   * @return {@link BlobStore}
   */
  @Bean
  @ConditionalOnProperty(name = "datastore.type", havingValue = "minio")
  public BlobStore minioBlobStore(@Value("${datastore.accessKey}") String accessKey,
      @Value("${datastore.secretKey}") String secretKey,
      @Value("${datastore.endpoint}") String endpoint) {

    BlobStoreContext blobStoreContext = ContextBuilder.newBuilder("s3")
        .endpoint(endpoint)
        .credentials(accessKey, secretKey)
        .buildView(BlobStoreContext.class);

    return blobStoreContext.getBlobStore();
  }

  /**
   * Creates DataStore bean to work with MinIO.
   *
   * @param blobStore          {@link BlobStore} object
   * @param bucketPrefix       Prefix for bucket name
   * @param defaultBucketName  Name of default bucket to use
   * @param region             Region to store
   * @param featureFlagHandler Instance of {@link FeatureFlagHandler} to check enabled features
   * @return {@link DataStore} object
   */
  @Bean
  @ConditionalOnProperty(name = "datastore.type", havingValue = "minio")
  public DataStore minioDataStore(@Autowired BlobStore blobStore,
      @Value("${datastore.bucketPrefix}") String bucketPrefix,
      @Value("${datastore.bucketPostfix}") String bucketPostfix,
      @Value("${datastore.defaultBucketName}") String defaultBucketName,
      @Value("${datastore.region}") String region, FeatureFlagHandler featureFlagHandler) {
    return new S3DataStore(
        blobStore, bucketPrefix, bucketPostfix, defaultBucketName, region, featureFlagHandler, false);
  }

  /**
   * Creates BlobStore bean to work with SeaweedFS.
   *
   * <p>Uses the generic {@code s3} jclouds provider with a custom {@code endpoint}. Signing is
   * forced to AWS Signature Version 4 ({@link S3Constants#PROPERTY_SIGNER_VERSION}) because SigV2 canonicalizes the
   * URL-encoded path (e.g. {@code /bucket/Azure%20DevOps}), while SeaweedFS verifies the signature against the decoded
   * path ({@code /bucket/Azure DevOps}), causing a {@code SignatureDoesNotMatch} → 403 →
   * {@code ProtocolException: Server rejected operation} during the {@code Expect: 100-continue} preflight.
   *
   * @param accessKey access key
   * @param secretKey secret key
   * @param endpoint  SeaweedFS S3 gateway endpoint URL
   * @param region    region used for SigV4 credential scope (must match the backend expectation, e.g.
   *                  {@code us-east-1})
   * @return {@link BlobStore}
   */
  @Bean
  @ConditionalOnProperty(name = "datastore.type", havingValue = "seaweedfs")
  public BlobStore seaweedFsBlobStore(@Value("${datastore.accessKey}") String accessKey,
      @Value("${datastore.secretKey}") String secretKey,
      @Value("${datastore.endpoint}") String endpoint,
      @Value("${datastore.region:eu-central-1}") String region) {

    Properties overrides = new Properties();
    overrides.setProperty(S3Constants.PROPERTY_S3_VIRTUAL_HOST_BUCKETS, "false");
    overrides.setProperty(S3Constants.PROPERTY_SIGNER_VERSION, "4");
    overrides.setProperty(LocationConstants.PROPERTY_REGION, region);

    BlobStoreContext blobStoreContext = ContextBuilder.newBuilder("s3")
        .endpoint(endpoint)
        .credentials(accessKey, secretKey)
        .overrides(overrides)
        .buildView(BlobStoreContext.class);

    return blobStoreContext.getBlobStore();
  }

  /**
   * Creates DataStore bean to work with SeaweedFS.
   *
   * @param blobStore          {@link BlobStore} object
   * @param bucketPrefix       prefix for bucket name
   * @param bucketPostfix      postfix for bucket name
   * @param defaultBucketName  name of default bucket to use
   * @param region             region to store
   * @param featureFlagHandler instance of {@link FeatureFlagHandler} to check enabled features
   * @return {@link DataStore} object
   */
  @Bean
  @ConditionalOnProperty(name = "datastore.type", havingValue = "seaweedfs")
  public DataStore seaweedFsDataStore(@Autowired BlobStore blobStore,
      @Value("${datastore.bucketPrefix}") String bucketPrefix,
      @Value("${datastore.bucketPostfix}") String bucketPostfix,
      @Value("${datastore.defaultBucketName}") String defaultBucketName,
      @Value("${datastore.region:eu-central-1}") String region, FeatureFlagHandler featureFlagHandler) {
    return new S3DataStore(
        blobStore, bucketPrefix, bucketPostfix, defaultBucketName, region, featureFlagHandler,
        true);
  }

  /**
   * Creates BlobStore bean, that works with AWS S3.
   *
   * @param accessKey accessKey to use
   * @param secretKey secretKey to use
   * @param region    AWS S3 region to use.
   * @return {@link BlobStore}
   */
  @Bean
  @ConditionalOnProperty(name = "datastore.type", havingValue = "s3")
  public BlobStore s3BlobStore(@Value("${datastore.accessKey}") String accessKey,
      @Value("${datastore.secretKey}") String secretKey,
      @Value("${datastore.region}") String region) {
    Iterable<Module> modules = ImmutableSet.of(new CustomBucketToRegionModule(region));

    BlobStoreContext blobStoreContext;
    if (StringUtils.isNotEmpty(accessKey) && StringUtils.isNotEmpty(secretKey)) {
      blobStoreContext = ContextBuilder.newBuilder("aws-s3")
          .modules(modules)
          .credentials(accessKey, secretKey)
          .buildView(BlobStoreContext.class);
    } else {
      blobStoreContext = ContextBuilder.newBuilder("aws-s3")
          .credentialsSupplier(new IAMCredentialSupplier())
          .modules(modules)
          .buildView(BlobStoreContext.class);
    }

    return blobStoreContext.getBlobStore();
  }

  @Bean
  @ConditionalOnProperty(name = "datastore.type", havingValue = "s3")
  public DataStore s3DataStore(@Autowired BlobStore blobStore,
      @Value("${datastore.bucketPrefix}") String bucketPrefix,
      @Value("${datastore.bucketPostfix}") String bucketPostfix,
      @Value("${datastore.defaultBucketName}") String defaultBucketName,
      @Value("${datastore.region}") String region, FeatureFlagHandler featureFlagHandler) {
    return new S3DataStore(blobStore, bucketPrefix, bucketPostfix, defaultBucketName, region,
        featureFlagHandler, false
    );
  }

  @Bean("attachmentThumbnailator")
  public Thumbnailator attachmentThumbnailator(
      @Value("${datastore.thumbnail.attachment.width}") int width,
      @Value("${datastore.thumbnail.attachment.height}") int height) {
    return new ThumbnailatorImpl(width, height);
  }

  @Bean("userPhotoThumbnailator")
  public Thumbnailator userPhotoThumbnailator(
      @Value("${datastore.thumbnail.avatar.width}") int width,
      @Value("${datastore.thumbnail.avatar.height}") int height) {
    return new ThumbnailatorImpl(width, height);
  }

}
