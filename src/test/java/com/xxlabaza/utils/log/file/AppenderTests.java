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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import io.appulse.utils.Bytes;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Testing appender functionality")
class AppenderTests {

  Path path = null;

  @BeforeEach
  void beforeAll () throws Exception {
    path = Files.createTempFile("log", ".removeme");
  }

  @AfterEach
  void afterAll () throws Exception {
    Files.delete(path);
  }

  @Test
  void append () throws Exception {
    val config = Config.builder()
        .path(path)
        .blockBufferSizeBytes(32)
        .forceFlush(false)
        .build();

    try (val appender = new Appender(config)) {
      val body = new byte[16];
      ThreadLocalRandom.current().nextBytes(body);
      val buffer = Bytes.wrap(body);

      val position = appender.append(buffer);
      assertThat(position).isGreaterThan(0);
    }
  }

  @Test
  void appendWithAlign () throws Exception {
    val config = Config.builder()
        .path(path)
        .blockBufferSizeBytes(32)
        .forceFlush(true)
        .build();

    val lastByteMarker = (byte) 7;
    val body = new byte[(config.getBlockBufferSizeBytes() - (Record.Header.BYTES * 2)) / 2];
    ThreadLocalRandom.current().nextBytes(body);
    body[body.length - 1] = lastByteMarker;

    val writeAction = (Runnable) () -> {
      try (val appender = new Appender(config)) {
        val buffer = Bytes.wrap(body);
        appender.append(buffer);
      }
    };
    val logFileSize = (Supplier<Integer>) () -> {
      try {
        val bytes = Files.readAllBytes(path);
        assertThat(bytes[bytes.length - 1]).isEqualTo(lastByteMarker);
        return bytes.length;
      } catch (IOException ex) {
        return -1;
      }
    };

    writeAction.run();
    assertThat(logFileSize.get())
        .isEqualTo(Header.BYTES + Record.Header.BYTES + body.length);

    writeAction.run();
    assertThat(logFileSize.get())
        .isEqualTo(Header.BYTES + config.getBlockBufferSizeBytes());

    writeAction.run();
    assertThat(logFileSize.get())
        .isEqualTo(Header.BYTES + config.getBlockBufferSizeBytes() + Record.Header.BYTES + body.length);

    writeAction.run();
    assertThat(logFileSize.get())
        .isEqualTo(Header.BYTES + config.getBlockBufferSizeBytes() + config.getBlockBufferSizeBytes());
  }
}
