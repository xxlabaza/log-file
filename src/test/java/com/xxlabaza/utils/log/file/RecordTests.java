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
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.CRC32;

import io.appulse.utils.Bytes;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Testing records static functions")
class RecordTests {

  @Test
  void write () {
    val destination = Bytes.allocate(32);

    val body = new byte[16];
    ThreadLocalRandom.current().nextBytes(body);
    val from = Bytes.wrap(body);

    Record.write(destination, FULL, from);

    val crc = new CRC32();
    crc.update(FULL.getCode());
    crc.update(new byte[] { 0x00, (byte) body.length });
    crc.update(body);
    val checksum = crc.getValue();

    assertThat(destination.arrayCopy()).containsExactly(Bytes.resizableArray()
        .write4B(checksum)
        .write1B(FULL.getCode())
        .write2B(body.length)
        .writeNB(body)
        .arrayCopy()
    );
  }

  @Test
  void read () {
    val blockBuffer = Bytes.allocate(32);

    val body = new byte[16];
    ThreadLocalRandom.current().nextBytes(body);
    val from = Bytes.wrap(body);

    Record.write(blockBuffer, FULL, from);

    val buffer = Bytes.resizableArray();
    Record.read(buffer, blockBuffer);

    assertThat(buffer.arrayCopy()).containsExactly(body);
  }

  @Test
  void getChecksum () {
    val block = Bytes.allocate(32);

    val body = new byte[16];
    ThreadLocalRandom.current().nextBytes(body);
    val buffer = Bytes.wrap(body);

    Record.write(block, FULL, buffer);

    val recordChecksum = Record.getChecksum(block);

    val crc = new CRC32();
    crc.update(FULL.getCode());
    crc.update(new byte[] { 0x00, (byte) body.length });
    crc.update(body);
    val calculatedChecksum = crc.getValue();

    assertThat(recordChecksum).isEqualTo(calculatedChecksum);
  }

  @Test
  void getType () {
    val block = Bytes.allocate(32);

    val body = new byte[16];
    ThreadLocalRandom.current().nextBytes(body);
    val buffer = Bytes.wrap(body);

    Record.write(block, FULL, buffer);
    val type = Record.getType(block);

    assertThat(type).isEqualTo(FULL);
  }

  @Test
  void getLength () {
    val block = Bytes.allocate(32);

    val body = new byte[16];
    ThreadLocalRandom.current().nextBytes(body);
    val buffer = Bytes.wrap(body);

    Record.write(block, FULL, buffer);
    val length = Record.getLength(block);

    assertThat(length).isEqualTo(body.length);
  }
}
