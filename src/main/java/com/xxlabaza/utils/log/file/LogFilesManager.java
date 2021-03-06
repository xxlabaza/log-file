/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xxlabaza.utils.log.file;

import static com.xxlabaza.utils.log.file.CorruptionHandler.PRINT_STACK_TRACE_AND_CONTINUE;
import static lombok.AccessLevel.PRIVATE;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.Semaphore;

import io.appulse.utils.Bytes;
import io.appulse.utils.BytesPool;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.With;
import lombok.experimental.FieldDefaults;
import lombok.val;

@FieldDefaults(level = PRIVATE, makeFinal = true)
public final class LogFilesManager {

  LogFilesManager.Config config;

  Map<Path, LogFile> logFiles;

  Semaphore readPermits;

  BytesPool pool;

  public LogFilesManager (LogFilesManager.Config config) {
    this.config = config;
    logFiles = new LruCache(config.getPermits().getWrite());
    readPermits = new Semaphore(config.getPermits().getRead());
    pool = BytesPool.builder()
        .initialBuffersCount(config.getPool().getInitialBuffersCount())
        .maximumBuffersCount(config.getPool().getMaximumBuffersCount())
        .initialBufferSizeBytes(config.getCommonConfig().getBlockBufferSizeBytes())
        .bufferCreateFunction(Bytes::allocate)
        .build();
  }

  @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
  public synchronized void append (@NonNull Path path, @NonNull Bytes buffer) {
    val logFile = logFiles.computeIfAbsent(path, this::createLogFile);
    logFile.append(buffer);
  }

  public void load (Path path, RecordConsumer consumer) {
    load(path, consumer, PRINT_STACK_TRACE_AND_CONTINUE);
  }

  @SneakyThrows
  public void load (@NonNull Path path, RecordConsumer consumer, CorruptionHandler corruptionHandler) {
    readPermits.acquire();
    try (val logFile = createLogFile(path)) {
      logFile.load(consumer, corruptionHandler);
    } finally {
      readPermits.release();
    }
  }

  private LogFile createLogFile (Path path) {
    val fullPath = config.getDirectory().resolve(path);
    val logFileConfig = config.getCommonConfig()
        .withPath(fullPath);

    return new LogFile(logFileConfig, pool);
  }

  @With
  @Value
  @Builder
  public static class Config {

    /**
     * The configuration with default settings.
     */
    public static final Config DEFAULT = Config.builder().build();

    @NonNull
    @Builder.Default
    Path directory = Paths.get("./");

    @NonNull
    @Builder.Default
    LogFile.Config commonConfig = LogFile.Config.DEFAULT;

    @NonNull
    @Builder.Default
    PermitsConfig permits = PermitsConfig.DEFAULT;

    @NonNull
    @Builder.Default
    PoolConfig pool = PoolConfig.DEFAULT;

    @With
    @Value
    @Builder
    public static class PermitsConfig {

      /**
       * The configuration with default settings.
       */
      public static final PermitsConfig DEFAULT = PermitsConfig.builder().build();

      @Builder.Default
      int write = 20;

      @Builder.Default
      int read = 20;
    }

    @With
    @Value
    @Builder
    public static class PoolConfig {

      /**
       * The configuration with default settings.
       */
      public static final PoolConfig DEFAULT = PoolConfig.builder().build();

      @Builder.Default
      int initialBuffersCount = 2;

      @Builder.Default
      int maximumBuffersCount = 1_000;
    }
  }
}
