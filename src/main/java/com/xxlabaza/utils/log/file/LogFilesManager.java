package com.xxlabaza.utils.log.file;

import static com.xxlabaza.utils.log.file.CorruptionHandler.PRINT_STACK_TRACE_AND_CONTINUE;
import static lombok.AccessLevel.PRIVATE;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Semaphore;
import java.util.LinkedHashMap;
import java.util.Map;

import io.appulse.utils.Bytes;
import io.appulse.utils.BytesPool;
import lombok.Builder;
import lombok.experimental.FieldDefaults;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.Value;
import lombok.With;

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

  public synchronized void append (@NonNull Path path, @NonNull Bytes buffer) {
    val logFile = logFiles.computeIfAbsent(path, this::createLogFile);
    logFile.append(buffer);
  }

  public void load (Path path, RecordConsumer consumer) {
    load(path, consumer, PRINT_STACK_TRACE_AND_CONTINUE);
  }

  @SneakyThrows
  public void load (@NonNull Path path, @NonNull RecordConsumer consumer, @NonNull CorruptionHandler corruptionHandler) {
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

  class LruCache extends LinkedHashMap<Path, LogFile> {

    private static final long serialVersionUID = 627252465946108735L;

    int maxSize;

    LruCache (int maxSize) {
      super(maxSize, 1.F, true);
      this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry (Map.Entry<Path, LogFile> eldest) {
      val shouldRemove = size() > maxSize;
      if (shouldRemove == true) {
        eldest.getValue().close();
      }
      return shouldRemove;
    }
  }

  @With
  @Value
  @Builder
  public static class Config {

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

      public static final PoolConfig DEFAULT = PoolConfig.builder().build();

      @Builder.Default
      int initialBuffersCount = 2;

      @Builder.Default
      int maximumBuffersCount = 1_000;
    }
  }
}
