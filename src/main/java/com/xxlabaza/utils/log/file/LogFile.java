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
import static io.appulse.utils.SizeUnit.KILOBYTES;
import static lombok.AccessLevel.PRIVATE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import com.xxlabaza.utils.log.file.exception.FileReadException;

import io.appulse.utils.Bytes;
import io.appulse.utils.BytesPool;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.With;
import lombok.experimental.FieldDefaults;
import lombok.val;

/**
 * The log file's management class, which allows two main operations:
 * <br/>
 * 1. to append a record to the file
 * <br/>
 * 2. to read all the records from the file's beginning
 */
@FieldDefaults(level = PRIVATE, makeFinal = true)
public final class LogFile implements AutoCloseable {

  Config config;
  AtomicInteger modificationCount;
  BytesPool pool;
  @Getter(value = PRIVATE, lazy = true)
  Appender appender = createAppender();

  /**
   * Constructs a new {@code LogFile} instance.
   *
   * @param config the configuration object.
   */
  public LogFile (Config config) {
    this(config, BytesPool.builder()
        .initialBuffersCount(1)
        .maximumBuffersCount(Integer.MAX_VALUE)
        .initialBufferSizeBytes(config.getBlockBufferSizeBytes())
        .bufferCreateFunction(Bytes::allocate)
        .build());
  }

  LogFile (Config config, BytesPool pool) {
    this.config = config;
    this.pool = pool;
    modificationCount = new AtomicInteger(0);
  }

  /**
   * Returns the file's path.
   *
   * @return the path to the file.
   */
  public Path path () {
    return config.getPath();
  }

  /**
   * Returns the file's size in <b>bytes</b>.
   *
   * @return the file size, in bytes.
   */
  @SneakyThrows
  public long size () {
    return Files.size(path());
  }

  /**
   * Appends data to the file.
   *
   * @param buffer the bytes, which need to append to the file.
   */
  public void append (@NonNull Bytes buffer) {
    modificationCount.incrementAndGet();
    getAppender().append(buffer);
  }

  /**
   * Reads all records from the file's beginning.
   *
   * @param consumer the client's logic for processing the readed data.
   */
  public void load (RecordConsumer consumer) {
    load(consumer, PRINT_STACK_TRACE_AND_CONTINUE);
  }

  /**
   * Reads all records from the file's beginning.
   *
   * @param consumer the client's logic for processing the readed data.
   *
   * @param corruptionHandler the corrupted data handler.
   *
   * @throws FileReadException in case of any read errors, except the corruptions -
   *                           they process with the specified handler
   */
  public void load (@NonNull RecordConsumer consumer, @NonNull CorruptionHandler corruptionHandler) {
    val builder = Reader.builder()
        .logFile(this)
        .config(config)
        .pool(pool);

    try (val reader = builder.build()) {
      reader.read(consumer, corruptionHandler);
    }
  }

  /**
   * Closes the file.
   */
  @Override
  public void close () {
    if (appender.get() == null) {
      return;
    }
    getAppender().close();
  }

  /**
   * Clears all data from the file.
   */
  public void clear () {
    if (appender.get() == null) {
      return;
    }
    getAppender().reset();
  }

  int getModificationCount () {
    return modificationCount.get();
  }

  private Appender createAppender () {
    return new Appender(config, pool);
  }

  /**
   * A log file's configuration object.
   */
  @With
  @Value
  @Builder
  public static class Config {

    /**
     * The configuration with default settings.
     */
    public static final Config DEFAULT = Config.builder().build();

    /**
     * A path to a log file. The default value is <b>./file.log</b>.
     *
     * @return the path to the log file.
     */
    @NonNull
    @Builder.Default
    Path path = Paths.get("./file.log");

    /**
     * A block buffer size, in bytes. It is an internal setting, which helps to
     * format the log file structure. The default value is <b>32 kilobytes</b>.
     *
     * @return the block buffer size, in bytes.
     */
    @NonNull
    @Builder.Default
    Integer blockBufferSizeBytes = (int) KILOBYTES.toBytes(32);

    /**
     * Forces any updates to the log file to be written to the storage device.
     * The default value is <b>true</b>.
     *
     * @return the current <b>forceFlush</b> value.
     */
    @NonNull
    @Builder.Default
    Boolean forceFlush = true;
  }
}
