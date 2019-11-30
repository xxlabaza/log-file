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

import static com.xxlabaza.utils.log.file.Record.Type.FULL;
import static com.xxlabaza.utils.log.file.Record.Type.UNDEFINED;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ThreadLocalRandom;

import io.appulse.utils.Bytes;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Testing block methods")
class BlockTests {

  @Test
  void writeFull () {
    val block = new Block(32);

    val body = new byte[16];
    ThreadLocalRandom.current().nextBytes(body);
    val buffer = Bytes.wrap(body);

    val continueWrite = block.write(buffer);
    assertThat(continueWrite).isFalse();
    assertThat(block.hasContent()).isTrue();
  }

  @Test
  void writeEmpty () {
    val block = new Block(32);
    val buffer = Bytes.allocate(16);

    val continueWrite = block.write(buffer);
    assertThat(continueWrite).isFalse();
    assertThat(block.hasContent()).isFalse();
  }

  @Test
  void writeManyFull () {
    val block = new Block(32);

    val body = new byte[8];
    for (int count = 0; count < 2; count++) {
      ThreadLocalRandom.current().nextBytes(body);
      val buffer = Bytes.wrap(body);

      val continueWrite = block.write(buffer);
      assertThat(continueWrite).isFalse();
    }
    assertThat(block.hasContent()).isTrue();
  }

  @Test
  void readFull () {
    val block = new Block(32);

    val body = new byte[16];
    ThreadLocalRandom.current().nextBytes(body);

    block.write(Bytes.wrap(body));

    val buffer = Bytes.resizableArray();
    val type = block.read(buffer);

    assertThat(type).isEqualTo(FULL);
    assertThat(buffer.writerIndex())
        .isEqualTo(body.length);
  }

  @Test
  void readEmpty () {
    val block = new Block(32);

    val buffer = Bytes.resizableArray();
    val type = block.read(buffer);

    assertThat(type).isEqualTo(UNDEFINED);
  }

  @Test
  void readManyFull () {
    val block = new Block(32);

    val body = new byte[8];
    for (int count = 0; count < 2; count++) {
      ThreadLocalRandom.current().nextBytes(body);
      val buffer = Bytes.wrap(body);
      block.write(buffer);
    }

    for (int count = 0; count < 2; count++) {
      val buffer = Bytes.resizableArray();
      val type = block.read(buffer);

      assertThat(type).isEqualTo(FULL);
      assertThat(buffer.writerIndex())
          .isEqualTo(body.length);
    }
  }

  @Test
  void reset () {
    val block = new Block(32);

    val body = new byte[16];
    ThreadLocalRandom.current().nextBytes(body);

    block.write(Bytes.wrap(body));
    assertThat(block.hasContent()).isTrue();

    block.reset();
    assertThat(block.hasContent()).isFalse();
  }
}
