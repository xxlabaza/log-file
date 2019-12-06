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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import io.appulse.utils.Bytes;
import lombok.val;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Testing log file class")
class LogFileTests {

  Path file;

  @BeforeEach
  void beforeAll () throws Exception {
    file = Files.createTempFile("test", ".removeme");
  }

  @AfterEach
  void afterAll () throws Exception {
    Files.deleteIfExists(file);
  }

  @Test
  void singleWriteAndRead () {
    val config = LogFile.Config.DEFAULT.withPath(file);
    try (val logFile = new LogFile(config)) {
      val payload = new byte[167];
      ThreadLocalRandom.current().nextBytes(payload);
      val buffer = Bytes.wrap(payload);

      logFile.append(buffer);
      assertThat(logFile.size()).isGreaterThan(0);

      val counter = new AtomicInteger();
      logFile.load((buf, pos) -> {
        assertThat(buf.arrayCopy()).containsExactly(payload);
        counter.incrementAndGet();
        return true;
      });

      assertThat(counter.intValue()).isEqualTo(1);
    }
  }

  @Test
  void multipleWritesAndReads () {
    val config = LogFile.Config.builder()
        .path(file)
        .blockBufferSizeBytes(32)
        .build();

    try (val logFile = new LogFile(config)) {
      val payload = new byte[20];
      ThreadLocalRandom.current().nextBytes(payload);

      logFile.append(Bytes.wrap(payload));
      logFile.append(Bytes.wrap(payload));
      logFile.append(Bytes.wrap(payload));

      val counter = new AtomicInteger();
      logFile.load((buffer, position) -> {
        assertThat(buffer.arrayCopy()).containsExactly(payload);
        counter.incrementAndGet();
        return true;
      });

      assertThat(counter.intValue()).isEqualTo(3);
    }
  }

  @Test
  void writeAndReadBigOne () {
    val config = LogFile.Config.builder()
        .path(file)
        .blockBufferSizeBytes(32)
        .build();

    try (val logFile = new LogFile(config)) {
      val payload = new byte[128];
      ThreadLocalRandom.current().nextBytes(payload);

      logFile.append(Bytes.wrap(payload));

      val counter = new AtomicInteger();
      logFile.load((buffer, position) -> {
        assertThat(buffer.arrayCopy()).containsExactly(payload);
        counter.incrementAndGet();
        return true;
      });

      assertThat(counter.intValue()).isEqualTo(1);
    }
  }

  @Test
  void writeAndReadWholeBlock () {
    val config = LogFile.Config.builder()
        .path(file)
        .blockBufferSizeBytes(32)
        .build();

    try (val logFile = new LogFile(config)) {
      val payload = new byte[32 - Record.Header.BYTES];
      ThreadLocalRandom.current().nextBytes(payload);

      logFile.append(Bytes.wrap(payload));
      logFile.append(Bytes.wrap(payload));
      logFile.append(Bytes.wrap(payload));

      val counter = new AtomicInteger();
      logFile.load((buffer, position) -> {
        assertThat(buffer.arrayCopy()).containsExactly(payload);
        counter.incrementAndGet();
        return true;
      });

      assertThat(counter.intValue()).isEqualTo(3);
    }
  }
}
